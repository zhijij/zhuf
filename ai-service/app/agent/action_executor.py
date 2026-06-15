from typing import Any

from app.agent.tools.rental_business import execute_action
from app.agent.tools.safety import require_confirmation


ALLOWED_ACTIONS = {
    "create_appointment",
    "favorite_house",
    "create_intention",
    "estimate_monthly_cost",
    "view_contract",
}


def execute_whitelisted_action(
    state: dict[str, Any],
    action: str,
    payload: dict[str, Any],
    confirmed: bool = False,
) -> dict[str, Any]:
    if action not in ALLOWED_ACTIONS:
        return {"success": False, "message": "动作不在 AI 白名单内", "action": action}
    confirmation = require_confirmation(action, confirmed)
    if confirmation["requiresConfirmation"]:
        return {"success": False, "confirmation": confirmation, "action": action}
    if action == "estimate_monthly_cost":
        rent = float(payload.get("rentAmount") or 0)
        deposit = float(payload.get("depositAmount") or rent)
        return {
            "success": True,
            "action": action,
            "result": {
                "firstMonthEstimated": rent + deposit,
                "monthlyRent": rent,
                "deposit": deposit,
            },
        }
    return execute_action(state, action, payload, confirmed=confirmed)
