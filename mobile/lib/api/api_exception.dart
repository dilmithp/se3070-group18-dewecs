enum ApiErrorKind { network, timeout, server, client }

/// The only error the API layer throws. [retryable] follows the client rules of contract v1 exactly:
/// retry later on network errors, timeouts, 5xx, 408 and 429; everything else needs the user.
class ApiException implements Exception {
  const ApiException({
    required this.kind,
    required this.detail,
    this.status,
    this.fieldErrors = const {},
  });

  final ApiErrorKind kind;
  final int? status;
  final String detail;
  final Map<String, String> fieldErrors;

  bool get retryable {
    switch (kind) {
      case ApiErrorKind.network:
      case ApiErrorKind.timeout:
      case ApiErrorKind.server:
        return true;
      case ApiErrorKind.client:
        return status == 408 || status == 429;
    }
  }

  /// The connection itself is down, so a sync run should stop instead of trying the next item.
  bool get isConnectionDown => kind == ApiErrorKind.network || kind == ApiErrorKind.timeout;

  @override
  String toString() => 'ApiException(${kind.name}, status: $status, detail: $detail)';
}
