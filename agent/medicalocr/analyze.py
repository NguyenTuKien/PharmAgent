from __future__ import annotations

from dataclasses import dataclass
from difflib import SequenceMatcher
from typing import Sequence
from uuid import uuid4

from pydantic import BaseModel, Field

from .catalog import CatalogProduct
from .normalize import collapse_whitespace, normalize_dosage, normalize_name


class OCRBlock(BaseModel):
    text: str
    bbox: list[int] | None = None
    confidence: float = 0.0


class OCRResult(BaseModel):
    engine: str = "manual"
    raw_text: str = ""
    display_text: str = ""
    confidence: float = 0.0
    blocks: list[OCRBlock] = Field(default_factory=list)


class MatchCandidate(BaseModel):
    product_id: str
    display_name: str
    normalized_name: str
    normalized_dosage: str = ""
    source_url: str = ""
    primary_image_url: str = ""
    source_category: str = ""
    brand_name: str = ""
    manufacturer_name: str = ""
    active_ingredient: str = ""
    packaging: str = ""
    indication: str = ""
    score: float = 0.0
    reasons: list[str] = Field(default_factory=list)


class MatchResult(BaseModel):
    decision: str
    query_text: str = ""
    normalized_query: str = ""
    normalized_dosage: str = ""
    best_match: MatchCandidate | None = None
    top_candidates: list[MatchCandidate] = Field(default_factory=list)
    match_reason: list[str] = Field(default_factory=list)


class UICandidate(BaseModel):
    product_id: str
    title: str
    image_url: str = ""
    score: float = 0.0


class UIResult(BaseModel):
    title: str = ""
    image_url: str = ""
    source_url: str = ""
    category: str = ""
    indication: str = ""
    top_candidates: list[UICandidate] = Field(default_factory=list)


class AnalyzeTextRequest(BaseModel):
    ocr_text: str
    top_k: int = 4
    request_id: str | None = None


class AnalyzeResponse(BaseModel):
    request_id: str
    ocr: OCRResult
    match: MatchResult
    ui: UIResult


@dataclass(frozen=True, slots=True)
class _PreparedQuery:
    text: str
    normalized_name: str
    normalized_dosage: str
    tokens: frozenset[str]
    identity_weights: dict[str, float]


def _token_jaccard(left: str, right: str) -> float:
    left_tokens = {token for token in normalize_name(left).split() if token}
    right_tokens = {token for token in normalize_name(right).split() if token}
    if not left_tokens or not right_tokens:
        return 0.0
    return len(left_tokens & right_tokens) / len(left_tokens | right_tokens)


_COMMON_NAME_STOPWORDS = {
    "thuoc",
    "vien",
    "vienn",
    "nang",
    "nen",
    "soi",
    "bot",
    "uong",
    "dung",
    "dich",
    "tablet",
    "tablets",
    "film",
    "coated",
    "capsule",
    "capsules",
    "contains",
    "effective",
    "pain",
    "relief",
    "giam",
    "dau",
    "ha",
    "sot",
    "tri",
    "ieu",
    "sieu",
    "phim",
    "hoa",
    "chat",
    "cong",
    "thuc",
    "dam",
    "dac",
    "vua",
    "nhe",
    "manh",
    "duoc",
    "suc",
}

_TITLE_HINT_SKIP_TOKENS = {
    "gsk",
    "stada",
    "dhg",
    "sanofi",
    "otc",
    "etc",
}

_VARIANT_TOKENS = {
    "coldflu",
    "codein",
    "codeine",
    "extra",
    "sinus",
}

_IDENTITY_TOKEN_MAX_PRODUCTS = 24


def _normalized_tokens(text: str) -> list[str]:
    return [token for token in normalize_name(text).split() if token]


def _product_name_tokens(product: CatalogProduct) -> set[str]:
    return {
        token
        for token in _normalized_tokens(product.normalized_name)
        if len(token) >= 4
        and token not in _COMMON_NAME_STOPWORDS
        and token not in _TITLE_HINT_SKIP_TOKENS
        and any(ch.isalpha() for ch in token)
    }


def _identity_token_counts(products: Sequence[CatalogProduct]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for product in products:
        for token in _product_name_tokens(product):
            counts[token] = counts.get(token, 0) + 1
    return counts


def _query_identity_weights(tokens: frozenset[str], counts: dict[str, int]) -> dict[str, float]:
    weights: dict[str, float] = {}
    for token in tokens:
        count = counts.get(token, 0)
        if count <= 0:
            continue
        if token not in _VARIANT_TOKENS and count > _IDENTITY_TOKEN_MAX_PRODUCTS:
            continue
        weights[token] = 1.0 / count
    return weights


def _strip_tokens(text: str, tokens_to_remove: set[str]) -> str:
    tokens = [token for token in _normalized_tokens(text) if token not in tokens_to_remove]
    return " ".join(tokens)


def _strip_normalized_tokens(text: str, tokens_to_remove: set[str]) -> str:
    tokens = [token for token in text.split() if token and token not in tokens_to_remove]
    return " ".join(tokens)


def _signal_tokens(text: str) -> set[str]:
    tokens: set[str] = set()
    for token in _normalized_tokens(text):
        if token in _COMMON_NAME_STOPWORDS or token in _TITLE_HINT_SKIP_TOKENS:
            continue
        if len(token) <= 2:
            continue
        if not any(ch.isalpha() for ch in token):
            continue
        tokens.add(token)
    return tokens


def _stop_tokens_for_product(product: CatalogProduct) -> set[str]:
    tokens = set(_COMMON_NAME_STOPWORDS)
    tokens.update(_normalized_tokens(product.active_ingredient))
    tokens.update(_normalized_tokens(product.packaging))
    return tokens


def _name_token_score(query: _PreparedQuery, product: CatalogProduct, stop_tokens: set[str]) -> float:
    product_tokens = _product_name_tokens(product) - stop_tokens
    if not product_tokens:
        return 0.0

    query_tokens = {
        token
        for token in query.tokens
        if len(token) >= 4
        and token not in _COMMON_NAME_STOPWORDS
        and token not in _TITLE_HINT_SKIP_TOKENS
        and any(ch.isalpha() for ch in token)
    }
    overlap = product_tokens & query_tokens
    if not overlap:
        return 0.0

    coverage = len(overlap) / len(product_tokens)
    strong_token = any(len(token) >= 5 for token in overlap)
    if strong_token:
        score = max(0.82, 0.72 + 0.22 * coverage)
    else:
        score = 0.65 + 0.20 * coverage

    query_variants = query_tokens & _VARIANT_TOKENS
    if query_variants and not query_variants <= product_tokens:
        score = min(score, 0.72)
    return score


def _apply_identity_token_score(name_score: float, query: _PreparedQuery, product: CatalogProduct) -> float:
    if not query.identity_weights:
        return name_score

    product_tokens = _product_name_tokens(product)
    query_variants = set(query.identity_weights) & _VARIANT_TOKENS
    if query_variants and not query_variants <= product_tokens:
        return min(name_score, 0.45)

    overlap = set(query.identity_weights) & product_tokens
    if overlap:
        matched_weight = sum(query.identity_weights[token] for token in overlap)
        total_weight = sum(query.identity_weights.values())
        coverage = matched_weight / total_weight if total_weight else 0.0
        return max(name_score, 0.78 + 0.18 * min(1.0, coverage))
    return min(name_score, 0.45)


def _extract_title_hint_from_text(text: str) -> str:
    tokens = _normalized_tokens(text)
    if not tokens:
        return ""

    hint_tokens: list[str] = []
    collecting = False
    for token in tokens:
        if token in _TITLE_HINT_SKIP_TOKENS:
            continue
        if token in _COMMON_NAME_STOPWORDS:
            if collecting:
                break
            continue
        if not any(ch.isalpha() for ch in token):
            continue
        if any(ch.isdigit() for ch in token):
            if collecting:
                break
            continue
        collecting = True
        hint_tokens.append(token)
        if len(hint_tokens) >= 3:
            break
    
    if not hint_tokens:
        for token in tokens:
            if (
                token not in _TITLE_HINT_SKIP_TOKENS
                and token not in _COMMON_NAME_STOPWORDS
                and any(ch.isalpha() for ch in token)
            ):
                hint_tokens.append(token)
                break

    return " ".join(hint_tokens)


def _infer_title_hint_from_blocks(blocks: Sequence[OCRBlock]) -> str:
    candidates: list[tuple[float, str]] = []
    if not blocks:
        return ""

    tops = [block.bbox[1] for block in blocks if block.bbox]
    areas = []
    for block in blocks:
        if not block.bbox:
            continue
        left, top, right, bottom = block.bbox
        areas.append(max(0, (right - left) * (bottom - top)))

    min_top = min(tops) if tops else 0.0
    max_top = max(tops) if tops else 0.0
    max_area = max(areas) if areas else 0.0

    for block in blocks:
        text = collapse_whitespace(block.text)
        hint = _extract_title_hint_from_text(text)
        if not hint:
            continue

        area_score = 0.0
        top_score = 0.0
        if block.bbox:
            left, top, right, bottom = block.bbox
            area = max(0, (right - left) * (bottom - top))
            area_score = (area / max_area) if max_area else 0.0
            top_score = 1.0
            if max_top > min_top:
                top_score = 1.0 - ((top - min_top) / (max_top - min_top))
                top_score = max(0.0, min(1.0, top_score))

        text_score = 0.0
        hint_tokens = hint.split()
        if 1 <= len(hint_tokens) <= 2:
            text_score = 1.0
        elif len(hint_tokens) == 3:
            text_score = 0.85
        else:
            text_score = 0.65

        if len(hint) <= 2:
            text_score *= 0.5
        if any(token.isdigit() for token in hint_tokens):
            text_score *= 0.7

        score = (
            0.60 * area_score
            + 0.20 * max(0.0, min(1.0, block.confidence))
            + 0.10 * top_score
            + 0.10 * text_score
        )
        candidates.append((score, hint))

    if not candidates:
        return ""

    candidates.sort(key=lambda item: (-item[0], len(item[1]), item[1]))
    return candidates[0][1]


def _score_text(query: str, target: str) -> float:
    query = collapse_whitespace(query).lower()
    target = collapse_whitespace(target).lower()
    if not query or not target:
        return 0.0
    if query == target:
        return 1.0
    if query in target or target in query:
        return 0.98
    token_score = _token_jaccard(query, target)
    if len(query) > 180 or len(target) > 180:
        return token_score
    return max(
        SequenceMatcher(None, query, target).ratio(),
        token_score,
    )


def _best_term_score(query: str, terms: Sequence[str]) -> tuple[float, str]:
    best_score = 0.0
    best_term = ""
    for term in terms:
        score = _score_text(query, term)
        if score > best_score:
            best_score = score
            best_term = term
    return best_score, best_term


def _build_ocr_result(raw_text: str) -> OCRResult:
    raw_text = collapse_whitespace(raw_text)
    blocks = [OCRBlock(text=raw_text, confidence=1.0)] if raw_text else []
    return OCRResult(
        engine="manual",
        raw_text=raw_text,
        confidence=1.0 if raw_text else 0.0,
        blocks=blocks,
    )


def _candidate_products(raw_text: str, products: Sequence[CatalogProduct], *, title_hint: str = "") -> list[CatalogProduct]:
    signal_tokens = _signal_tokens(raw_text)
    if title_hint:
        signal_tokens.update(_signal_tokens(title_hint))

    if not signal_tokens:
        return list(products)

    candidates = [product for product in products if not product.search_tokens.isdisjoint(signal_tokens)]
    return candidates or list(products)


def _score_product(query: _PreparedQuery, product: CatalogProduct, *, title_hint: str = "") -> MatchCandidate:
    name_terms = [product.normalized_name]
    stop_tokens = _stop_tokens_for_product(product)
    filtered_query = _strip_normalized_tokens(query.normalized_name, stop_tokens)
    filtered_name_terms = [_strip_tokens(term, stop_tokens) for term in name_terms]

    raw_name_score, raw_matched_name = _best_term_score(query.normalized_name, name_terms)
    filtered_name_score, filtered_matched_name = _best_term_score(filtered_query, filtered_name_terms)
    token_name_score = _name_token_score(query, product, stop_tokens)
    title_hint_score = 0.0
    if title_hint:
        title_hint_score = max(
            _score_text(title_hint, product.display_name),
            _score_text(title_hint, product.normalized_name),
        )
    if token_name_score >= title_hint_score and token_name_score >= raw_name_score and token_name_score >= filtered_name_score:
        name_score = token_name_score
        matched_name = product.normalized_name
    elif title_hint_score >= raw_name_score and title_hint_score >= filtered_name_score:
        name_score = title_hint_score
        matched_name = product.normalized_name
    elif filtered_name_score > raw_name_score:
        name_score = filtered_name_score
        matched_name = filtered_matched_name
    else:
        name_score = raw_name_score
        matched_name = raw_matched_name

    query_variants = query.tokens & _VARIANT_TOKENS
    product_name_tokens = set(_normalized_tokens(product.normalized_name))
    if query_variants and not query_variants <= product_name_tokens:
        name_score = min(name_score, 0.72)
    name_score = _apply_identity_token_score(name_score, query, product)

    dosage_score = _score_text(query.normalized_dosage, product.normalized_dosage) if query.normalized_dosage or product.normalized_dosage else 0.0
    ingredient_score = _score_text(query.normalized_name, product.active_ingredient)
    brand_score = _score_text(query.normalized_name, product.brand_name)
    packaging_score = _score_text(query.normalized_name, product.packaging)
    category_score = _score_text(query.normalized_name, product.source_category)

    final_score = (
        0.70 * name_score
        + 0.20 * dosage_score
        + 0.05 * ingredient_score
        + 0.03 * brand_score
        + 0.01 * packaging_score
        + 0.01 * category_score
    )

    reasons: list[str] = []
    if matched_name:
        reasons.append("normalized_name")
    if dosage_score >= 0.8 and query.normalized_dosage:
        reasons.append("normalized_dosage")
    if ingredient_score >= 0.8 and product.active_ingredient:
        reasons.append("active_ingredient")
    if brand_score >= 0.8 and product.brand_name:
        reasons.append("brand_name")
    if packaging_score >= 0.8 and product.packaging:
        reasons.append("packaging")
    if category_score >= 0.8 and product.source_category:
        reasons.append("source_category")
    if not reasons and final_score > 0:
        reasons.append("fuzzy_match")

    return MatchCandidate(
        product_id=product.product_id,
        display_name=product.display_name,
        normalized_name=product.normalized_name,
        normalized_dosage=product.normalized_dosage,
        source_url=product.source_url,
        primary_image_url=product.primary_image_url,
        source_category=product.source_category,
        brand_name=product.brand_name,
        manufacturer_name=product.manufacturer_name,
        active_ingredient=product.active_ingredient,
        packaging=product.packaging,
        indication=product.indication,
        score=round(final_score, 4),
        reasons=reasons,
    )


def match_text(
    raw_text: str,
    products: Sequence[CatalogProduct],
    *,
    top_k: int = 4,
    title_hint: str = "",
) -> MatchResult:
    query_text = collapse_whitespace(raw_text)
    normalized_query = normalize_name(query_text)
    normalized_dosage = normalize_dosage(query_text)
    tokens = frozenset(token for token in normalized_query.split() if token)
    identity_counts = _identity_token_counts(products)
    prepared_query = _PreparedQuery(
        text=query_text,
        normalized_name=normalized_query,
        normalized_dosage=normalized_dosage,
        tokens=tokens,
        identity_weights=_query_identity_weights(tokens, identity_counts),
    )

    if not query_text or (not normalized_query and not normalized_dosage):
        return MatchResult(
            decision="not_found",
            query_text=query_text,
            normalized_query=normalized_query,
            normalized_dosage=normalized_dosage,
        )

    candidate_products = _candidate_products(query_text, products, title_hint=title_hint)
    scored = [_score_product(prepared_query, product, title_hint=title_hint) for product in candidate_products]
    scored.sort(key=lambda item: (-item.score, item.display_name.lower(), item.product_id))

    top_candidates = scored[: max(1, top_k)]
    best_match = top_candidates[0] if top_candidates else None
    second_score = top_candidates[1].score if len(top_candidates) > 1 else 0.0
    gap = (best_match.score - second_score) if best_match else 0.0
    strong_match = bool(
        best_match
        and "normalized_dosage" in best_match.reasons
        and "normalized_name" in best_match.reasons
    )

    if best_match is None or best_match.score < 0.3:
        decision = "not_found"
    elif strong_match and best_match.score >= 0.9:
        decision = "accept"
    elif best_match.score >= 0.85 and gap >= 0.10:
        decision = "accept"
    else:
        decision = "review"

    return MatchResult(
        decision=decision,
        query_text=query_text,
        normalized_query=normalized_query,
        normalized_dosage=normalized_dosage,
        best_match=best_match,
        top_candidates=top_candidates,
        match_reason=best_match.reasons if best_match else [],
    )


def _build_ui_result(match: MatchResult) -> UIResult:
    if not match.best_match:
        return UIResult(
            title="",
            image_url="",
            category="",
            indication="",
            top_candidates=[
                UICandidate(
                    product_id=item.product_id,
                    title=item.display_name,
                    image_url=item.primary_image_url,
                    score=item.score,
                )
                for item in match.top_candidates
            ],
        )

    best = match.best_match
    return UIResult(
        title=best.display_name,
        image_url=best.primary_image_url,
        source_url=best.source_url,
        category=best.source_category,
        indication=best.indication,
        top_candidates=[
            UICandidate(
                product_id=item.product_id,
                title=item.display_name,
                image_url=item.primary_image_url,
                score=item.score,
            )
            for item in match.top_candidates
        ],
    )


def analyze_ocr_result(
    ocr: OCRResult,
    products: Sequence[CatalogProduct],
    *,
    top_k: int = 4,
    request_id: str | None = None,
) -> AnalyzeResponse:
    title_hint = _infer_title_hint_from_blocks(ocr.blocks)
    match_text_input = ocr.display_text or ocr.raw_text
    match = match_text(match_text_input, products, top_k=top_k, title_hint=title_hint)
    ui = _build_ui_result(match)
    return AnalyzeResponse(
        request_id=request_id or uuid4().hex,
        ocr=ocr,
        match=match,
        ui=ui,
    )


def analyze_text(
    raw_text: str,
    products: Sequence[CatalogProduct],
    *,
    top_k: int = 4,
    request_id: str | None = None,
) -> AnalyzeResponse:
    return analyze_ocr_result(
        _build_ocr_result(raw_text),
        products,
        top_k=top_k,
        request_id=request_id,
    )
