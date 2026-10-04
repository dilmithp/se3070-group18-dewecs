import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { get } from './api.js';

/** GET a JSON page (a null path fetches nothing); returns { data, error, loading, reload }. */
export function useApi(path) {
  const [state, setState] = useState({ data: null, error: null, loading: !!path });
  const [tick, setTick] = useState(0);
  useEffect(() => {
    if (!path) {
      setState({ data: null, error: null, loading: false });
      return undefined;
    }
    let live = true;
    setState((s) => ({ ...s, loading: true, error: null }));
    get(path)
      .then((data) => live && setState({ data, error: null, loading: false }))
      .catch((error) => live && setState({ data: null, error, loading: false }));
    return () => {
      live = false;
    };
  }, [path, tick]);
  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { ...state, reload };
}

const ToastContext = createContext(() => {});
export const ToastProvider = ToastContext.Provider;
export const useToast = () => useContext(ToastContext);

/** Runs an action, toasts its message or error, then calls onDone. */
export function useAction(onDone) {
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const run = async (fn, confirmText) => {
    if (confirmText && !window.confirm(confirmText)) return;
    setBusy(true);
    try {
      const res = await fn();
      toast(res?.message || 'Done.', 'ok');
      onDone?.(res);
    } catch (e) {
      toast(e.message, 'error');
    } finally {
      setBusy(false);
    }
  };
  return { run, busy };
}

/** The officer acting in this browser (the backend has no login yet; actions name a user id). */
export function useOfficer() {
  const [id, setIdState] = useState(() => {
    try {
      return localStorage.getItem('dewecs.officerId') || '';
    } catch {
      return '';
    }
  });
  const setId = (v) => {
    setIdState(v);
    try {
      localStorage.setItem('dewecs.officerId', v);
    } catch {
      /* ignore */
    }
  };
  return [id, setId];
}

/** Form state + submit that surfaces the backend's fieldErrors / rule violations. */
export function useForm(initial) {
  const [values, setValues] = useState(initial);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [busy, setBusy] = useState(false);
  const set = (k) => (v) => setValues((s) => ({ ...s, [k]: v?.target ? v.target.value : v }));
  const submit = (fn) => async (e) => {
    e.preventDefault();
    setBusy(true);
    setErrors({});
    setFormError(null);
    try {
      await fn(values);
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setFormError(err.message);
    } finally {
      setBusy(false);
    }
  };
  return { values, setValues, set, errors, formError, busy, submit };
}
