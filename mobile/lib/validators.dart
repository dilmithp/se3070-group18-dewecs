import 'models/contract_rules.dart';
import 'strings.dart';

/// Client-side checks that mirror the contract rules exactly. Each returns an error message, or null when valid.
String? validateNic(String? value) {
  final nic = normaliseNic(value ?? '');
  if (nic.isEmpty) {
    return S.nicRequired;
  }
  return nicPattern.hasMatch(nic) ? null : S.nicInvalid;
}

String? validateFullName(String? value) {
  final name = (value ?? '').trim();
  if (name.isEmpty) {
    return S.nameRequired;
  }
  return name.length > maxNameLength ? S.nameTooLong : null;
}

String? validatePhone(String? value) {
  final phone = value ?? '';
  if (phone.trim().isEmpty) {
    return S.phoneRequired;
  }
  return phonePattern.hasMatch(phone) ? null : S.phoneInvalid;
}

String? validateDistrict(Object? value) => value == null ? S.districtRequired : null;

/// A web address with an http or https scheme and a host.
String? validateBaseUrl(String? value) {
  final uri = Uri.tryParse((value ?? '').trim());
  final ok = uri != null && (uri.scheme == 'http' || uri.scheme == 'https') && uri.host.isNotEmpty;
  return ok ? null : S.baseUrlInvalid;
}

String? validateDescription(String? value) {
  final text = (value ?? '').trim();
  if (text.isEmpty) {
    return S.descriptionRequired;
  }
  return text.length > maxDescriptionLength ? S.descriptionTooLong : null;
}

/// Accepts a decimal point or a decimal comma. Null when the text is not a number.
double? parseCoordinate(String? text) {
  final cleaned = (text ?? '').trim().replaceAll(',', '.');
  if (cleaned.isEmpty) {
    return null;
  }
  final value = double.tryParse(cleaned);
  return value != null && value.isFinite ? value : null;
}

String? validateLatitude(String? value) {
  if ((value ?? '').trim().isEmpty) {
    return S.latRequired;
  }
  final lat = parseCoordinate(value);
  if (lat == null) {
    return S.latInvalid;
  }
  return lat < -90 || lat > 90 ? S.latRange : null;
}

/// No Sri Lanka bounding box on purpose: an emulator sits in California by default.
String? validateLongitude(String? value) {
  if ((value ?? '').trim().isEmpty) {
    return S.lngRequired;
  }
  final lng = parseCoordinate(value);
  if (lng == null) {
    return S.lngInvalid;
  }
  return lng < -180 || lng > 180 ? S.lngRange : null;
}
