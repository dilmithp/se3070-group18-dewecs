/// Something that keeps data on the phone and can wipe it (used when Demo mode is switched).
abstract interface class Clearable {
  Future<void> clearLocalData();
}
