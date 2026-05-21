from __future__ import annotations

from dataclasses import dataclass
from dataclasses import field

from .normalize import collapse_whitespace, normalize_name


def _normalize_optional_text(value: object) -> str:
    if value is None:
        return ""
    if isinstance(value, str):
        return collapse_whitespace(value)
    return collapse_whitespace(str(value))


def _token_set(*values: str) -> frozenset[str]:
    tokens: set[str] = set()
    for value in values:
        tokens.update(token for token in normalize_name(value).split() if token)
    return frozenset(tokens)


@dataclass(slots=True)
class CatalogProduct:
    source_url: str
    display_name: str
    normalized_name: str
    normalized_dosage: str = ""
    source_category: str = ""
    brand_name: str = ""
    manufacturer_name: str = ""
    active_ingredient: str = ""
    packaging: str = ""
    indication: str = ""
    primary_image_url: str = ""
    search_tokens: frozenset[str] = field(default_factory=frozenset, repr=False)

    @property
    def product_id(self) -> str:
        return self.source_url

    @classmethod
    def from_json(cls, raw: dict[str, object]) -> "CatalogProduct":
        raw_display_name = _normalize_optional_text(raw.get("display_name"))
        raw_normalized_name = _normalize_optional_text(raw.get("normalized_name"))
        display_name = raw_display_name or raw_normalized_name
        normalized_name = raw_normalized_name or normalize_name(display_name)
        normalized_dosage = _normalize_optional_text(raw.get("normalized_dosage"))
        source_category = _normalize_optional_text(raw.get("source_category"))
        brand_name = _normalize_optional_text(raw.get("brand_name"))
        manufacturer_name = _normalize_optional_text(raw.get("manufacturer_name"))
        active_ingredient = _normalize_optional_text(raw.get("active_ingredient"))
        packaging = _normalize_optional_text(raw.get("packaging"))
        return cls(
            source_url=_normalize_optional_text(raw.get("source_url")),
            display_name=display_name,
            normalized_name=normalized_name,
            normalized_dosage=normalized_dosage,
            source_category=source_category,
            brand_name=brand_name,
            manufacturer_name=manufacturer_name,
            active_ingredient=active_ingredient,
            packaging=packaging,
            indication=_normalize_optional_text(raw.get("indication")),
            primary_image_url=_normalize_optional_text(raw.get("primary_image_url")),
            search_tokens=_token_set(
                display_name,
                normalized_name,
                normalized_dosage,
                source_category,
                brand_name,
                manufacturer_name,
                active_ingredient,
                packaging,
            ),
        )
