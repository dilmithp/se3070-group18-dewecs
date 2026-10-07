/// Keeps a picked photo somewhere that survives while the report waits in the queue.
abstract class PhotoStore {
  /// Copies the picked file into the store and returns the new path.
  Future<String> save(String sourcePath);

  Future<List<int>> read(String path);

  /// Best effort; a missing file is fine.
  Future<void> delete(String path);
}

/// Thrown by a store when the photo file is not there any more.
class PhotoMissingException implements Exception {
  const PhotoMissingException(this.path);

  final String path;

  @override
  String toString() => 'Photo missing: $path';
}

/// In-memory store for tests (and for a web build, where files cannot be kept).
class MemoryPhotoStore implements PhotoStore {
  final Map<String, List<int>> files = {};
  int _next = 1;

  /// Pretends the picker produced a file at [path].
  void put(String path, List<int> bytes) => files[path] = bytes;

  @override
  Future<String> save(String sourcePath) async {
    final bytes = files[sourcePath];
    if (bytes == null) {
      throw PhotoMissingException(sourcePath);
    }
    final stored = 'memory://photo-${_next++}';
    files[stored] = bytes;
    return stored;
  }

  @override
  Future<List<int>> read(String path) async {
    final bytes = files[path];
    if (bytes == null) {
      throw PhotoMissingException(path);
    }
    return bytes;
  }

  @override
  Future<void> delete(String path) async => files.remove(path);
}
