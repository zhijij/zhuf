MEDIUM_HIGH_RISK_ACTIONS = {
    "create_appointment",
    "create_intention",
    "view_contract",
}

DESTRUCTIVE_ACTIONS = {
    "cancel_appointment",
    "abandon_intention",
    "terminate_contract",
}


def require_confirmation(action: str, confirmed: bool = False) -> dict:
    risk = "high" if action in DESTRUCTIVE_ACTIONS else "medium" if action in MEDIUM_HIGH_RISK_ACTIONS else "low"
    required = risk in {"medium", "high"} and not confirmed
    return {
        "action": action,
        "risk": risk,
        "confirmed": confirmed,
        "requiresConfirmation": required,
        "message": "该动作需要用户确认后执行。" if required else "该动作可继续执行。",
    }
