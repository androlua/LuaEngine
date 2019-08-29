# LuaEngine
LuaEngine is an Lua Script Engine on Android，maybe work on Android , it based on AndroLua+(nirenr)

How to use?
pass two paths of lua module path and c lib path
dofile can run a script file
kotlin:
luaEngine = LuaEngine(App.luaLpath, App.luaCpath).apply {
                onReceiveErrorListener = object : LuaEngine.OnReceiveErrorListener {
                    override fun onReceiveError(title: String?, msg: java.lang.Exception?) {
                        val s=title.toString() + ":" + msg.toString()
                        toast(s)
                        loge("lua",s)
                    }
                }
                onReceiveMessageListener = object : LuaEngine.OnReceiveMessageListener {
                    override fun onReceiveMessage(msg: String?) {
                        toast(msg.toString())
                        loge("lua",msg.toString())
                    }
                }
            }
            initLuaJava()
            luaEngine.doFile(App.luaMdDir + "/main.lua")
			
			
you can use java api in lua like this:

require "import"

import "java.io.File"
File("/sdcard/a.txt").delete()