from typing import Any

from langchain_core.tools import StructuredTool

from app.agent.tools import amap, rental_business, safety, vector_search


def tool_specs() -> list[dict[str, str]]:
    return [
        {"name": "search_houses", "description": "Search public rental houses through Java business boundary."},
        {"name": "get_house_detail", "description": "Fetch a tenant-visible house detail."},
        {"name": "get_contract", "description": "Fetch a contract detail visible to current user."},
        {"name": "vector_search", "description": "Search Milvus or fallback pgvector knowledge."},
        {"name": "amap_search_poi", "description": "Search nearby POI via AMap when configured."},
        {"name": "amap_estimate_route", "description": "Estimate commute route by AMap coordinates."},
        {"name": "amap_search_around", "description": "Search nearby POI around coordinates through Java business boundary."},
        {"name": "amap_house_context", "description": "Fetch AMap commute and nearby context for a selected house through Java boundary."},
        {"name": "require_confirmation", "description": "Check whether an action requires human confirmation."},
    ]


def build_langchain_tools() -> list[Any]:
    return [
        StructuredTool.from_function(name="search_houses", func=rental_business.search_houses),
        StructuredTool.from_function(name="get_house_detail", func=rental_business.get_house_detail),
        StructuredTool.from_function(name="get_contract", func=rental_business.get_contract),
        StructuredTool.from_function(name="vector_search", func=vector_search.vector_search),
        StructuredTool.from_function(name="amap_search_poi", func=amap.search_poi),
        StructuredTool.from_function(name="amap_estimate_route", func=amap.estimate_route),
        StructuredTool.from_function(name="amap_search_around", func=rental_business.search_amap_around),
        StructuredTool.from_function(name="amap_house_context", func=rental_business.get_house_map_context),
        StructuredTool.from_function(name="require_confirmation", func=safety.require_confirmation),
    ]
