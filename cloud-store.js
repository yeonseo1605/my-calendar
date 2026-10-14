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

  async function init(onRemoteState) {
    if (!configured || !window.supabase?.createClient) return { configured: false, signedIn: false };
    client = window.supabase.createClient(config.supabaseUrl, config.supabaseAnonKey, {
      auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true }
    });
    const { data } = await client.auth.getSession();
    session = data.session || null;
    user = session?.user || null;
    client.auth.onAuthStateChange((_event, nextSession) => {
      session = nextSession || null;
      user = session?.user || null;
      window.dispatchEvent(new CustomEvent('my-calendar-auth-changed', { detail: {
        signedIn: !!user,
        providerToken: session?.provider_token || ''
      }}));
    });
    if (!user) return { configured: true, signedIn: false };

    const { data: row, error } = await client
      .from('user_settings')
      .select('payload,schema_version,updated_at')
      .eq('user_id', user.id)
      .maybeSingle();
    if (error) throw error;
    const hadRemote = !!(row?.payload && Object.keys(row.payload).length);
    if (hadRemote) {
      applyingRemote = true;
      await onRemoteState(row.payload);
      applyingRemote = false;
    }
    return { configured: true, signedIn: true, user, hadRemote, providerToken: session?.provider_token || '' };
  }

  function queueSave(payload) {
    if (!client || !user || applyingRemote) return;
    clearTimeout(saveTimer);
    saveTimer = setTimeout(async () => {
      const { error } = await client.from('user_settings').upsert({
        user_id: user.id,
        payload,
        schema_version: 1
      }, { onConflict: 'user_id' });
      if (error) console.error('My Calendar cloud save failed:', error.message);
    }, 700);
  }

  async function signIn() {
    if (!client) throw new Error('Supabase 연결 설정이 필요합니다.');
    const redirectTo = `${location.origin}${location.pathname}`;
    const { error } = await client.auth.signInWithOAuth({
      provider: 'google',
      options: {
        redirectTo,
        scopes: 'openid email profile https://www.googleapis.com/auth/calendar.events',
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
    currentUser: () => user,
    providerToken: () => session?.provider_token || ''
  };
})();
