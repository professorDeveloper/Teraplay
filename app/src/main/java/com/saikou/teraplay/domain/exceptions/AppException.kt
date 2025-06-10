package com.saikou.teraplay.domain.exceptions

sealed class AppException(message: String) : Exception(message)

class NetworkException(message: String) : AppException(message)
class ApiException(message: String) : AppException(message)
class NotFoundException(message: String) : AppException(message)
class ServerException(message: String) : AppException(message)
class UnknownException(message: String) : AppException(message)