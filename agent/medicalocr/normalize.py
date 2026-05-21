from __future__ import annotations

import re
import unicodedata

WHITESPACE_RE = re.compile(r"\s+")
PARENS_RE = re.compile(r"\([^)]*\)")
DOSAGE_RE = re.compile(
    r"(?<![\d.])(\d+(?:[.,]\d+)?)\s*(mg|g|mcg|µg|ug|ml|iu|%)(?:\s*/\s*(mg|g|mcg|µg|ug|ml|iu|%))?(?![A-Za-z0-9])",
    re.IGNORECASE,
)
INLINE_DOSAGE_RE = re.compile(
    r"(?<![\d.])(\d+(?:[.,]\d+)?)\s*(mg|g|mcg|µg|ug|ml|iu|%)(?:\s*/\s*(mg|g|mcg|µg|ug|ml|iu|%))?(?![A-Za-z0-9])",
    re.IGNORECASE,
)
NAME_DOSAGE_FRAGMENT_RE = re.compile(
    r"(?<!\d)\d+(?:[.,]\d+)?\s*(mg|g|mcg|µg|ug|ml|iu|%)(?=(?:\d|[\s+/.,()]|$))",
    re.IGNORECASE,
)
NAME_ALIASES = {
    "cold flu": "coldflu",
    "cam cum": "coldflu",
    "flu": "coldflu",
    "cf": "coldflu",
    "natriclorid": "natri clorid",
    "natricloride": "natri clorid",
    "nacl": "natri clorid",
    "sodium chloride": "natri clorid",
}


def collapse_whitespace(text: str) -> str:
    return WHITESPACE_RE.sub(" ", text).strip()


def strip_accents(text: str) -> str:
    normalized = unicodedata.normalize("NFD", text)
    return "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")


def normalize_name(text: str) -> str:
    if not text:
        return ""

    text = collapse_whitespace(text)
    text = strip_accents(text).lower()
    text = text.replace("đ", "d").replace("Đ", "d")
    text = PARENS_RE.sub(" ", text)
    text = DOSAGE_RE.sub(" ", text)
    text = INLINE_DOSAGE_RE.sub(" ", text)
    text = text.replace("/", " ")
    while True:
        next_text = NAME_DOSAGE_FRAGMENT_RE.sub(" ", text)
        if next_text == text:
            break
        text = next_text
    text = re.sub(r"\b\d+\s*x\s*\d+\b", " ", text)
    text = re.sub(r"\b\d+x\d+\b", " ", text)
    text = re.sub(r"\b\d+\s*x\b", " ", text)
    text = re.sub(r"\bx\s*\d+\b", " ", text)
    text = re.sub(r"\bx\b", " ", text)
    text = text.replace(",", " ")
    text = re.sub(r"[^a-z0-9+/ ]+", " ", text)
    text = collapse_whitespace(text)
    for source, target in NAME_ALIASES.items():
        text = re.sub(rf"\b{re.escape(source)}\b", target, text)
    return collapse_whitespace(text)


def normalize_dosage(text: str) -> str:
    if not text:
        return ""

    text = strip_accents(text).lower()
    text = text.replace("µ", "u")
    matches: list[str] = []
    for regex in (DOSAGE_RE, INLINE_DOSAGE_RE):
        for value, unit_a, unit_b in regex.findall(text):
            numeric_value = float(value.replace(",", "."))
            if numeric_value <= 0:
                continue
            unit_a = unit_a.lower()
            unit_a = "mcg" if unit_a in {"ug", "µg"} else unit_a
            if unit_b:
                unit_b = unit_b.lower()
                unit_b = "mcg" if unit_b in {"ug", "µg"} else unit_b
                token = f"{value.replace(',', '.')} {unit_a}/{unit_b}"
            else:
                token = f"{value.replace(',', '.')} {unit_a}"
            if token not in matches:
                matches.append(token)

    return " + ".join(matches)
