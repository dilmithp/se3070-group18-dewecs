import 'dart:io';

import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

import 'photo_store.dart';

/// Copies picked photos into the app documents folder (the picker's temporary file may disappear).
class FilePhotoStore implements PhotoStore {
  Future<Directory> _directory() async {
    final documents = await getApplicationDocumentsDirectory();
    final directory = Directory(p.join(documents.path, 'report_photos'));
    if (!await directory.exists()) {
      await directory.create(recursive: true);
    }
    return directory;
  }

  @override
  Future<String> save(String sourcePath) async {
    final directory = await _directory();
    final extension = p.extension(sourcePath).isEmpty ? '.jpg' : p.extension(sourcePath);
    final target = p.join(directory.path, '${DateTime.now().microsecondsSinceEpoch}$extension');
    await File(sourcePath).copy(target);
    return target;
  }

  @override
  Future<List<int>> read(String path) => File(path).readAsBytes();

  @override
  Future<void> delete(String path) async {
    try {
      final file = File(path);
      if (await file.exists()) {
        await file.delete();
      }
    } on FileSystemException {
      // best effort
    }
  }
}
