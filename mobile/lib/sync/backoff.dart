/// Wait before the next attempt after [attempts] failures: 5 s, 15 s, 60 s, 5 min, then every 5 min.
Duration backoffFor(int attempts) {
  const steps = [
    Duration(seconds: 5),
    Duration(seconds: 15),
    Duration(seconds: 60),
    Duration(minutes: 5),
  ];
  if (attempts <= 1) {
    return steps.first;
  }
  return attempts - 1 < steps.length ? steps[attempts - 1] : steps.last;
}
