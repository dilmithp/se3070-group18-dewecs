import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_exception.dart';
import '../state/identity_controller.dart';
import '../state/reference_data_controller.dart';
import '../strings.dart';
import '../validators.dart';

/// First-run (and "Identify again") form: NIC, name, phone, district. The NIC is sent but never stored.
class IdentifyScreen extends StatefulWidget {
  const IdentifyScreen({super.key});

  @override
  State<IdentifyScreen> createState() => _IdentifyScreenState();
}

class _IdentifyScreenState extends State<IdentifyScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nic = TextEditingController();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  int? _districtId;
  bool _busy = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    // Started after the first frame: refresh() notifies listeners, which is not allowed during build.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      final reference = context.read<ReferenceDataController>();
      if (reference.data == null) {
        reference.refresh();
      }
    });
  }

  @override
  void dispose() {
    _nic.dispose();
    _name.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_busy || !_formKey.currentState!.validate()) {
      return;
    }
    setState(() {
      _busy = true;
      _message = null;
    });
    final identity = context.read<IdentityController>();
    final messenger = ScaffoldMessenger.of(context);
    final navigator = Navigator.of(context);
    try {
      await identity.identify(
        nic: _nic.text,
        fullName: _name.text,
        phone: _phone.text,
        districtId: _districtId!,
      );
      messenger.showSnackBar(const SnackBar(content: Text(S.identifyDone)));
      navigator.pop(true);
    } on ApiException catch (e) {
      if (!mounted) {
        return;
      }
      setState(() {
        _busy = false;
        _message = e.isConnectionDown ? S.identifyOffline : _describe(e);
      });
    }
  }

  String _describe(ApiException e) {
    if (e.fieldErrors.isEmpty) {
      return e.detail;
    }
    return '${e.detail}\n${e.fieldErrors.values.join('\n')}';
  }

  @override
  Widget build(BuildContext context) {
    final reference = context.watch<ReferenceDataController>();
    final districts = reference.data?.districts ?? const [];
    return Scaffold(
      appBar: AppBar(title: const Text(S.identifyTitle)),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: [
              const Text(S.identifyIntro),
              const SizedBox(height: 16),
              TextFormField(
                controller: _nic,
                decoration: const InputDecoration(labelText: S.nicLabel, helperText: S.nicHelp),
                textCapitalization: TextCapitalization.characters,
                autocorrect: false,
                validator: validateNic,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _name,
                decoration: const InputDecoration(labelText: S.nameLabel),
                textCapitalization: TextCapitalization.words,
                validator: validateFullName,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _phone,
                decoration: const InputDecoration(labelText: S.phoneLabel),
                keyboardType: TextInputType.phone,
                validator: validatePhone,
              ),
              const SizedBox(height: 16),
              if (districts.isEmpty) ...[
                Text(reference.loading ? S.loading : S.districtsMissing),
                const SizedBox(height: 8),
                OutlinedButton(
                  onPressed: reference.loading ? null : reference.refresh,
                  child: const Text(S.reloadDistricts),
                ),
              ] else
                DropdownButtonFormField<int>(
                  initialValue: _districtId,
                  decoration: const InputDecoration(labelText: S.districtLabel),
                  items: [
                    for (final d in districts) DropdownMenuItem(value: d.id, child: Text(d.name)),
                  ],
                  onChanged: (value) => setState(() => _districtId = value),
                  validator: validateDistrict,
                ),
              if (_message != null) ...[
                const SizedBox(height: 16),
                Semantics(
                  liveRegion: true,
                  child: Text(_message!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
                ),
              ],
              const SizedBox(height: 24),
              FilledButton(
                onPressed: _busy || districts.isEmpty ? null : _submit,
                child: _busy
                    ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Text(S.identifyButton),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
