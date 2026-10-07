/// Rules of contract v1 shared by the form validators and the fake server.
final nicPattern = RegExp(r'^(\d{9}[VX]|\d{12})$');
final phonePattern = RegExp(r'^\+?[0-9][0-9 -]{7,14}$');

const maxNameLength = 120;
const maxDescriptionLength = 2000;
const maxPhotoBytes = 5 * 1024 * 1024;

/// The server trims and upper-cases the NIC before checking it.
String normaliseNic(String nic) => nic.trim().toUpperCase();

/// Photo statuses the server accepts an upload in.
const photoAllowedStatuses = {'PENDING_SYNC', 'PENDING_REVIEW', 'NEEDS_INFO'};
