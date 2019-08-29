package com.androlua;

import android.content.Intent;
import android.os.StrictMode;

import com.luajava.JavaFunction;
import com.luajava.LuaException;
import com.luajava.LuaObject;
import com.luajava.LuaState;
import com.luajava.LuaStateFactory;

import java.util.ArrayList;

public class LuaEngine implements LuaContext {

    private final static String DATA = "data";

    private LuaState L;
    private String luaCpath;
    //getFilesDir

    private String luaLpath;

    private boolean mDebug = true;
    private ArrayList<LuaGcable> gclist = new ArrayList<>();


    public LuaEngine(String lpath, String cpath) {
        LuaEnhancer.dexedPath = lpath;
        luaLpath = lpath;
        luaCpath = cpath;
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());

        try {
            initLua();
        } catch (Exception e) {
            sendMsg(e.getMessage());
        }
    }


    @Override
    public void regGc(LuaGcable obj) {
        gclist.add(obj);
    }

    @Override
    public String getLuaLpath() {
        return luaLpath;
    }

    @Override
    public String getLuaCpath() {
        return luaCpath;
    }

    @Override
    public LuaState getLuaState() {
        return L;
    }

    protected void onDestroy() {
        System.gc();
        L.gc(LuaState.LUA_GCCOLLECT, 1);
        //L.close();
    }

    public LuaAsyncTask newTask(LuaObject func) throws LuaException {
        return newTask(func, null, null);
    }

    public LuaAsyncTask newTask(LuaObject func, LuaObject callback) throws LuaException {
        return newTask(func, null, callback);
    }

    public LuaAsyncTask newTask(LuaObject func, LuaObject update, LuaObject callback) throws LuaException {
        return new LuaAsyncTask(this, func, update, callback);
    }

    public LuaThread newThread(LuaObject func) throws LuaException {
        return newThread(func, null);
    }

    public LuaThread newThread(LuaObject func, Object[] arg) throws LuaException {
        LuaThread thread = new LuaThread(this, func, true, arg);
        return thread;
    }

    public LuaTimer newTimer(LuaObject func) throws LuaException {
        return newTimer(func, null);
    }

    public LuaTimer newTimer(LuaObject func, Object[] arg) throws LuaException {
        return new LuaTimer(this, func, arg);
    }

    public LuaAsyncTask task(long delay, LuaObject func) throws LuaException {
        return task(delay, null, null);
    }

    public LuaAsyncTask task(long delay, Object[] arg, LuaObject func) throws LuaException {
        LuaAsyncTask task = new LuaAsyncTask(this, delay, func);
        task.execute(arg);
        return task;
    }

    public LuaAsyncTask task(LuaObject func) throws LuaException {
        return task(func, null, null, null);
    }

    public LuaAsyncTask task(LuaObject func, Object[] arg) throws LuaException {
        return task(func, arg, null, null);
    }

    public LuaAsyncTask task(LuaObject func, Object[] arg, LuaObject callback) throws LuaException {
        return task(func, null, null, callback);
    }

    public LuaAsyncTask task(LuaObject func, LuaObject update, LuaObject callback) throws LuaException {
        return task(func, null, update, callback);
    }

    public LuaAsyncTask task(LuaObject func, Object[] arg, LuaObject update, LuaObject callback) throws LuaException {
        LuaAsyncTask task = new LuaAsyncTask(this, func, update, callback);
        task.execute(arg);
        return task;
    }

    public LuaThread thread(LuaObject func) throws LuaException {
        LuaThread thread = newThread(func, null);
        thread.start();
        return thread;
    }

    public LuaThread thread(LuaObject func, Object[] arg) throws LuaException {
        LuaThread thread = new LuaThread(this, func, true, arg);
        thread.start();
        return thread;
    }

    public LuaTimer timer(LuaObject func, long period) throws LuaException {
        return timer(func, 0, period, null);
    }

    public LuaTimer timer(LuaObject func, long period, Object[] arg) throws LuaException {
        return timer(func, 0, period, arg);
    }

    public LuaTimer timer(LuaObject func, long delay, long period) throws LuaException {
        return timer(func, delay, period, null);
    }

    public LuaTimer timer(LuaObject func, long delay, long period, Object[] arg) throws LuaException {
        LuaTimer timer = new LuaTimer(this, func, arg);
        timer.start(delay, period);
        return timer;
    }

    public Ticker ticker(final LuaObject func, long period) throws LuaException {
        Ticker timer = new Ticker();
        timer.setOnTickListener(new Ticker.OnTickListener() {
            @Override
            public void onTick() {
                try {
                    func.call();
                } catch (LuaException e) {
                    e.printStackTrace();
                    sendError("onTick", e);
                }
            }
        });
        timer.start();
        timer.setPeriod(period);
        return timer;
    }


    //初始化lua使用的Java函数
    private void initLua() throws Exception {
        L = LuaStateFactory.newLuaState();
        L.openLibs();
        L.pushJavaObject(this);
        L.setGlobal("engine");
        L.getGlobal("engine");
        L.setGlobal("this");
        L.pushContext(this);
        L.getGlobal("luajava");
        L.pop(1);

        JavaFunction print = new LuaPrint(this, L);
        print.register("print");

        L.getGlobal("package");
        L.pushString(luaLpath);
        L.setField(-2, "path");
        L.pushString(luaCpath);
        L.setField(-2, "cpath");
        L.pop(1);

        JavaFunction set = new JavaFunction(L) {
            @Override
            public int execute() throws LuaException {
                LuaThread thread = (LuaThread) L.toJavaObject(2);

                thread.set(L.toString(3), L.toJavaObject(4));
                return 0;
            }
        };
        set.register("set");

        JavaFunction call = new JavaFunction(L) {
            @Override
            public int execute() throws LuaException {
                LuaThread thread = (LuaThread) L.toJavaObject(2);

                int top = L.getTop();
                if (top > 3) {
                    Object[] args = new Object[top - 3];
                    for (int i = 4; i <= top; i++) {
                        args[i - 4] = L.toJavaObject(i);
                    }
                    thread.call(L.toString(3), args);
                } else if (top == 3) {
                    thread.call(L.toString(3));
                }

                return 0;
            }
        };
        call.register("call");
    }

    public void setDebug(boolean isDebug) {
        mDebug = isDebug;
    }

    //运行lua脚本
    public Object doFile(String filePath) {
        return doFile(filePath, new Object[0]);
    }

    public Object doFile(String filePath, Object[] args) {
        int ok = 0;
        try {
            L.setTop(0);
            ok = L.LloadFile(filePath);

            if (ok == 0) {
                L.getGlobal("debug");
                L.getField(-1, "traceback");
                L.remove(-2);
                L.insert(-2);
                int l = args.length;
                for (int i = 0; i < l; i++) {
                    L.pushObjectValue(args[i]);
                }
                ok = L.pcall(l, 1, -2 - l);
                if (ok == 0) {
                    return L.toJavaObject(-1);
                }
            }
            throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        } catch (LuaException e) {
            sendMsg(e.getMessage());

        }
        return null;
    }
/*
    public Object doAsset(String name, Object... args) {
        int ok = 0;
        try {
            byte[] bytes = readAsset(name);
            L.setTop(0);
            ok = L.LloadBuffer(bytes, name);

            if (ok == 0) {
                L.getGlobal("debug");
                L.getField(-1, "traceback");
                L.remove(-2);
                L.insert(-2);
                int l = args.length;
                for (int i = 0; i < l; i++) {
                    L.pushObjectValue(args[i]);
                }
                ok = L.pcall(l, 0, -2 - l);
                if (ok == 0) {
                    return L.toJavaObject(-1);
                }
            }
            throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        } catch (Exception e) {
            setTitle(errorReason(ok));
            sendMsg(e.getMessage());
        }
        return null;
    }*/

    //运行lua函数
    public Object runFunc(String funcName, Object... args) {
        if (L != null) {
            synchronized (L) {
                try {
                    L.setTop(0);
                    L.pushGlobalTable();
                    L.pushString(funcName);
                    L.rawGet(-2);
                    if (L.isFunction(-1)) {
                        L.getGlobal("debug");
                        L.getField(-1, "traceback");
                        L.remove(-2);
                        L.insert(-2);

                        int l = args.length;
                        for (int i = 0; i < l; i++) {
                            L.pushObjectValue(args[i]);
                        }

                        int ok = L.pcall(l, 1, -2 - l);
                        if (ok == 0) {
                            return L.toJavaObject(-1);
                        }
                        throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
                    }
                } catch (LuaException e) {
                    sendError(funcName, e);
                }
            }
        }
        return null;
    }

    //运行lua代码
    public Object doString(String funcSrc, Object... args) {
        try {
            L.setTop(0);
            int ok = L.LloadString(funcSrc);

            if (ok == 0) {
                L.getGlobal("debug");
                L.getField(-1, "traceback");
                L.remove(-2);
                L.insert(-2);

                int l = args.length;
                for (Object arg : args) {
                    L.pushObjectValue(arg);
                }

                ok = L.pcall(l, 1, -2 - l);
                if (ok == 0) {
                    return L.toJavaObject(-1);
                }
            }
            throw new LuaException(errorReason(ok) + ": " + L.toString(-1));
        } catch (LuaException e) {
            sendMsg(e.getMessage());
        }
        return null;
    }

//读取asset文件

    //生成错误信息
    private String errorReason(int error) {
        switch (error) {
            case 6:
                return "error error";
            case 5:
                return "GC error";
            case 4:
                return "Out of memory";
            case 3:
                return "Syntax error";
            case 2:
                return "Runtime error";
            case 1:
                return "Yield error";
        }
        return "Unknown error " + error;
    }


    public OnReceiveErrorListener onReceiveErrorListener;
    public OnReceiveMessageListener onReceiveMessageListener;

    //显示信息
    public void sendMsg(String msg) {
        if (onReceiveMessageListener != null)
            onReceiveMessageListener.onReceiveMessage(msg);
    }

    @Override
    public void sendError(String title, Exception msg) {
        if (onReceiveErrorListener != null)
            onReceiveErrorListener.onReceiveError(title, msg);
    }


    private void setField(String key, Object value) {
        synchronized (L) {
            try {
                L.pushObjectValue(value);
                L.setGlobal(key);
            } catch (LuaException e) {
                sendError("setField", e);
            }
        }
    }

    public void call(String func) {
        runFunc(func);
    }

    public void call(String func, Object[] args) {
        if (args.length == 0)
            runFunc(func);
        else
            runFunc(func, args);
    }

    public void set(String key, Object value) {
        setField(key, value);
    }

    public Object get(String key) throws LuaException {
        synchronized (L) {
            L.getGlobal(key);
            return L.toJavaObject(-1);
        }
    }

    public interface OnReceiveErrorListener {
        void onReceiveError(String title, Exception msg);
    }

    public interface OnReceiveMessageListener {
        void onReceiveMessage(String msg);
    }
}
