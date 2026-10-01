import 'package:image_picker/image_picker.dart';

enum PhotoSource { camera, gallery }

/// Camera or gallery behind a small interface so tests can plug in a stub.
abstract class PhotoPicker {
  /// The path of the picked file, or null when the user cancelled.
  Future<String?> pick(PhotoSource source);
}

/// maxWidth 1600 and quality 80 keep a phone photo far below the server's 5 MB limit.
class ImagePickerPhotoPicker implements PhotoPicker {
  final ImagePicker _picker = ImagePicker();

  @override
  Future<String?> pick(PhotoSource source) async {
    final file = await _picker.pickImage(
      source: source == PhotoSource.camera ? ImageSource.camera : ImageSource.gallery,
      maxWidth: 1600,
      imageQuality: 80,
    );
    return file?.path;
  }
}
