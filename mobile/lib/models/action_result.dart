/// The answer to an officer action (create, close, assign ...): the server's message and where the record lives.
class ActionResult {
  const ActionResult({required this.message, required this.location});

  final String message;

  /// Path of the affected record, for example `/shelters/7`; the id is its last segment.
  final String location;

  /// The record id taken from [location], or null when the path does not end in a number.
  int? get id => int.tryParse(location.split('/').where((s) => s.isNotEmpty).lastOrNull ?? '');

  factory ActionResult.fromJson(Map<String, dynamic> json) =>
      ActionResult(message: (json['message'] as String?) ?? '', location: (json['location'] as String?) ?? '');
}
