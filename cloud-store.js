(function () {
  const config = window.MY_CALENDAR_CONFIG || {};
  const configured = /^https:\/\/.+\.supabase\.co$/.test(config.supabaseUrl || '')
    && config.supabaseAnonKey
    && !config.supabaseAnonKey.startsWith('YOUR_');
  let client = null;
  let user = null;
  let session = null;
  let saveTimer = null;
  let applyingRemote = false;
  let remoteHandler = null;
  let lastRemoteUpdatedAt = '';
  let refreshTimer = null;

  async function loadRemote(force = false) {
    if (!client || !user || !remoteHandler) return { hadRemote: false };
    const { data: row, error } = await client.from('user_settings')
      .select('payload,schema_version,updated_at').eq('user_id', user.id).maybeSingle();
    if (error) throw error;
    const hadRemote = !!(row?.payload && Object.keys(row.payload).length);
    if (hadRemote && (force || row.updated_at !== lastRemoteUpdatedAt)) {
      lastRemoteUpdatedAt = row.updated_at || '';
      applyingRemote = true;
      try { await remoteHandler(row.payload); } finally { applyingRemote = false; }
    }
    return { hadRemote };
  }

  function startRefresh() {
    clearInterval(refreshTimer);
    if (!user) return;
    refreshTimer = setInterval(() => loadRemote().catch(error =>
      console.error('My Calendar cloud refresh failed:', error.message)
    ), 10000);
  }

  async function init(onRemoteState) {
    if (!configured || !window.supabase?.createClient) return { configured: false, signedIn: false };
    client = window.supabase.createClient(config.supabaseUrl, config.supabaseAnonKey, {
      auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true }
    });
    remoteHandler = onRemoteState;
    const { data } = await client.auth.getSession();
    session = data.session || null;
    user = session?.user || null;
    client.auth.onAuthStateChange((event, nextSession) => {
      session = nextSession || null;
      user = session?.user || null;
      if (user) setTimeout(() => { loadRemote(event === 'SIGNED_IN').catch(console.error); startRefresh(); }, 0);
      else { clearInterval(refreshTimer); lastRemoteUpdatedAt = ''; }
      window.dispatchEvent(new CustomEvent('my-calendar-auth-changed', { detail: {
        signedIn: !!user,
        providerToken: session?.provider_token || '',
        user
      }}));
    });
    if (!user) return { configured: true, signedIn: false };
    const { hadRemote } = await loadRemote(true);
    startRefresh();
    return { configured: true, signedIn: true, user, hadRemote, providerToken: session?.provider_token || '' };
  }

  function queueSave(payload) {
    if (!client || !user || applyingRemote) return;
    clearTimeout(saveTimer);
    saveTimer = setTimeout(async () => {
      const { data, error } = await client.from('user_settings').upsert({
        user_id: user.id,
        payload,
        schema_version: 1
      }, { onConflict: 'user_id' }).select('updated_at').single();
      if (error) console.error('My Calendar cloud save failed:', error.message);
      else lastRemoteUpdatedAt = data?.updated_at || lastRemoteUpdatedAt;
    }, 700);
  }

  async function signIn() {
    if (!client) throw new Error('Supabase 연결 설정이 필요합니다.');
    const redirectTo = `${location.origin}${location.pathname}`;
    const { error } = await client.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo,
        scopes: 'openid email profile https://www.googleapis.com/auth/calendar.events https://www.googleapis.com/auth/calendar.calendarlist.readonly',
        queryParams: { access_type: 'offline', prompt: 'consent' }
      }
    });
    if (error) throw error;
  }

  async function signOut() {
    if (!client) return;
    const { error } = await client.auth.signOut();
    if (error) throw error;
    user = null;
    session = null;
  }

  window.MyCalendarCloud = {
    configured: () => configured,
    init,
    queueSave,
    signIn,
    signOut,
    refresh: () => loadRemote(true),
    currentUser: () => user,
    providerToken: () => session?.provider_token || ''
  };
})();
