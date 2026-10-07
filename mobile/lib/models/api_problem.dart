/// RFC 7807 problem body. Parsing is lenient: a missing field never throws.
class ApiProblem {
  const ApiProblem({this.status, this.title, this.detail, this.fieldErrors = const {}});

  final int? status;
  final String? title;
  final String? detail;
  final Map<String, String> fieldErrors;

  factory ApiProblem.fromJson(Map<String, dynamic> json) {
    final raw = json['fieldErrors'];
    final errors = <String, String>{};
    if (raw is Map) {
      raw.forEach((key, value) => errors[key.toString()] = value.toString());
    }
    final status = json['status'];
    return ApiProblem(
      status: status is num ? status.toInt() : null,
      title: json['title']?.toString(),
      detail: json['detail']?.toString(),
      fieldErrors: errors,
    );
  }
}
