from fastapi import HTTPException, status


class SafeWalkException(Exception):
    """应用基础异常类"""
    pass


class UserNotFound(SafeWalkException):
    """用户不存在异常"""
    pass


class LocationNotFound(SafeWalkException):
    """定位记录不存在异常"""
    pass


class EmergencyNotFound(SafeWalkException):
    """紧急事件不存在异常"""
    pass


class InvalidCredentials(SafeWalkException):
    """无效的凭证异常"""
    pass


class UserAlreadyExists(SafeWalkException):
    """用户已存在异常"""
    pass


def handle_safewalk_exception(exc: SafeWalkException) -> HTTPException:
    """处理SafeWalk异常"""
    if isinstance(exc, UserNotFound):
        return HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )
    elif isinstance(exc, LocationNotFound):
        return HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Location not found"
        )
    elif isinstance(exc, EmergencyNotFound):
        return HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Emergency not found"
        )
    elif isinstance(exc, InvalidCredentials):
        return HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials"
        )
    elif isinstance(exc, UserAlreadyExists):
        return HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="User already exists"
        )
    else:
        return HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )
