from __future__ import annotations

import sqlite3
from pathlib import Path

from .catalog import CatalogProduct

DEFAULT_DB_PATH = Path(__file__).resolve().parents[1] / "data" / "medicalocr.sqlite3"

PRODUCT_COLUMNS = (
    "source_url",
    "display_name",
    "normalized_name",
    "normalized_dosage",
    "source_category",
    "brand_name",
    "manufacturer_name",
    "active_ingredient",
    "packaging",
    "indication",
    "primary_image_url",
)


def load_catalog_products_from_db(db_path: str | Path = DEFAULT_DB_PATH) -> list[CatalogProduct]:
    path = Path(db_path)
    if not path.exists():
        return []

    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    try:
        rows = conn.execute(
            f"""
            select {", ".join(PRODUCT_COLUMNS)}
            from products
            order by id asc
            """
        ).fetchall()
    finally:
        conn.close()

    products: list[CatalogProduct] = []
    for row in rows:
        product = CatalogProduct.from_json({column: row[column] for column in PRODUCT_COLUMNS})
        if (
            product.source_url
            and product.display_name
            and product.normalized_name
            and product.indication
            and product.primary_image_url
        ):
            products.append(product)
    return products
