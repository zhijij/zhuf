import time
from collections import defaultdict, deque
from functools import wraps
from typing import Any, Callable

import httpx


class GuardrailError(RuntimeError):
    pass


_RATE_BUCKETS: dict[str, deque[float]] = defaultdict(deque)


def rate_limit(key: str, limit: int = 30, window_seconds: int = 60) -> None:
    now = time.time()
    bucket = _RATE_BUCKETS[key]
    while bucket and now - bucket[0] > window_seconds:
        bucket.popleft()
    if len(bucket) >= limit:
        raise GuardrailError("请求过于频繁，请稍后再试")
    bucket.append(now)


def guarded(
    *,
    name: str,
    retries: int = 1,
    timeout_seconds: float = 8,
    retry_base_seconds: float = 0.25,
) -> Callable[[Callable[..., Any]], Callable[..., Any]]:
    def decorator(func: Callable[..., Any]) -> Callable[..., Any]:
        @wraps(func)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            last_error: Exception | None = None
            for attempt in range(retries + 1):
                started = time.time()
                try:
                    result = func(*args, **kwargs)
                    if time.time() - started > timeout_seconds:
                        raise GuardrailError(f"{name} 执行超时")
                    return result
                except (httpx.HTTPError, TimeoutError, GuardrailError) as exc:
                    last_error = exc
                    if attempt >= retries:
                        break
                    time.sleep(retry_base_seconds * (2 ** attempt))
                except Exception as exc:
                    last_error = exc
                    break
            raise GuardrailError(f"{name} 调用失败：{str(last_error)[:160]}")

        return wrapper

    return decorator


def safe_call(
    func: Callable[..., Any],
    *,
    name: str,
    retries: int = 0,
    **kwargs: Any,
) -> Any:
    return guarded(name=name, retries=retries)(func)(**kwargs)
