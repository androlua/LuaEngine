package com.androlua;

import com.luajava.LuaState;

public interface LuaContext {

    public void call(String func, Object... args);

    public void set(String name, Object value);

    public String getLuaLpath();

    public String getLuaCpath();

    public LuaState getLuaState();

    public Object doFile(String path, Object... arg);

    public void sendMsg(String msg);

    public void sendError(String title, Exception msg);

    public void regGc(LuaGcable obj);

}
