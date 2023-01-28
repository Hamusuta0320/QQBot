package com.cxj.plugins.wuziqi

class R<T> {
    private var _code: Int = 200
    private var _err: String? = null
    private var _data: T? = null

    fun data(data: T?): R<T> {
        this._data = data
        return this
    }

    fun code(code: Int): R<T> {
        this._code = code
        return this
    }

    fun err(err: String?): R<T> {
        this._err = err
        return this
    }

    fun getCode(): Int = _code
    fun getData(): T? = _data
    fun getErr(): String? = _err

    companion object {
        fun <T> error(code: Int, err: String): R<T> {
            return R<T>().code(code).err(err)
        }

        fun <T> ok(data: T? = null): R<T> {
            return R<T>().code(200).data(data)
        }
    }
}

