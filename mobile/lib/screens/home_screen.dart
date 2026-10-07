import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/queued_report.dart';
import '../models/report_item.dart';
import '../state/reports_controller.dart';
import '../strings.dart';
import '../sync/sync_service.dart';
import '../widgets/report_card.dart';
import 'report_detail_screen.dart';

/// My reports: waiting items first, then items sent but not yet listed by the server, then the server list.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.onNewReport});

  final VoidCallback onNewReport;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    // After the first frame: both calls notify listeners, which is not allowed during build.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _refresh(force: false);
      }
    });
  }

  Future<void> _refresh({bool force = true}) async {
    final sync = context.read<SyncService>();
    final reports = context.read<ReportsController>();
    await sync.syncNow(force: force);
    await reports.refresh();
  }

  void _open(ReportItem item) {
    Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => ReportDetailScreen(item: item)));
  }

  @override
  Widget build(BuildContext context) {
    final reports = context.watch<ReportsController>();
    final sync = context.watch<SyncService>();
    final theme = Theme.of(context);

    final serverReports = reports.items;
    final serverIds = serverReports.map((r) => r.id).toSet();
    int byNewest(QueuedReport a, QueuedReport b) => b.createdAtMs.compareTo(a.createdAtMs);
    final unsent = sync.items.where((i) => i.isActive || i.state == QueueState.needsAttention).toList()..sort(byNewest);
    final sentLocal = sync.items
        .where((i) => i.state == QueueState.synced && !serverIds.contains(i.serverId))
        .toList()
      ..sort(byNewest);
    final waiting = sync.items.where((i) => i.isActive).length;
    final nothing = unsent.isEmpty && sentLocal.isEmpty && serverReports.isEmpty;

    final children = <Widget>[
      if (reports.offline)
        _Banner(text: S.offlineBanner, icon: Icons.cloud_off, color: theme.colorScheme.tertiaryContainer)
      else if (reports.error != null && !nothing)
        _Banner(
          text: S.refreshFailed(reports.error!.detail),
          icon: Icons.error_outline,
          color: theme.colorScheme.errorContainer,
        ),
      if (waiting > 0)
        _WaitingBar(count: waiting, sending: sync.running, onSendNow: () => sync.syncNow(force: true)),
      for (final q in unsent) ReportCard(item: ReportItem.fromQueued(q), onTap: () => _open(ReportItem.fromQueued(q))),
      for (final q in sentLocal) ReportCard(item: ReportItem.fromQueued(q), onTap: () => _open(ReportItem.fromQueued(q))),
      for (final r in serverReports) ReportCard(item: ReportItem.fromServer(r), onTap: () => _open(ReportItem.fromServer(r))),
      if (reports.hasMore)
        Padding(
          padding: const EdgeInsets.all(12),
          child: OutlinedButton(
            onPressed: reports.loadingMore ? null : reports.loadMore,
            child: reports.loadingMore
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text(S.loadMore),
          ),
        ),
    ];

    final Widget content;
    if (nothing && reports.loading) {
      content = const Center(child: CircularProgressIndicator());
    } else if (nothing && reports.error != null && !reports.offline) {
      content = _Message(
        icon: Icons.error_outline,
        title: S.unexpectedError,
        body: reports.error!.detail,
        actionLabel: S.retry,
        onAction: () => _refresh(),
      );
    } else if (nothing && reports.offline) {
      content = _Message(
        icon: Icons.cloud_off,
        title: S.offlineBanner,
        body: S.noConnection,
        actionLabel: S.retry,
        onAction: () => _refresh(),
      );
    } else if (nothing) {
      content = _Message(
        icon: Icons.inbox_outlined,
        title: S.homeEmptyTitle,
        body: S.homeEmptyBody,
        actionLabel: S.homeEmptyAction,
        onAction: widget.onNewReport,
      );
    } else {
      content = ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 24),
        children: children,
      );
    }

    return RefreshIndicator(
      onRefresh: _refresh,
      // The empty and error states are scrollable too, so pull-to-refresh works there.
      child: nothing && !reports.loading
          ? LayoutBuilder(
              builder: (context, constraints) => SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                child: SizedBox(height: constraints.maxHeight, child: content),
              ),
            )
          : content,
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.text, required this.icon, required this.color});

  final String text;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      child: Container(
        color: color,
        padding: const EdgeInsets.all(12),
        child: Row(children: [Icon(icon), const SizedBox(width: 8), Expanded(child: Text(text))]),
      ),
    );
  }
}

class _WaitingBar extends StatelessWidget {
  const _WaitingBar({required this.count, required this.sending, required this.onSendNow});

  final int count;
  final bool sending;
  final VoidCallback onSendNow;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      child: Row(
        children: [
          Expanded(child: Text(S.waitingToSend(count))),
          FilledButton.tonal(onPressed: sending ? null : onSendNow, child: const Text(S.sendNow)),
        ],
      ),
    );
  }
}

class _Message extends StatelessWidget {
  const _Message({required this.icon, required this.title, required this.body, required this.actionLabel, required this.onAction});

  final IconData icon;
  final String title;
  final String body;
  final String actionLabel;
  final VoidCallback onAction;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 56, color: Theme.of(context).colorScheme.primary),
            const SizedBox(height: 16),
            Text(title, style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center),
            const SizedBox(height: 8),
            Text(body, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            FilledButton(onPressed: onAction, child: Text(actionLabel)),
          ],
        ),
      ),
    );
  }
}
