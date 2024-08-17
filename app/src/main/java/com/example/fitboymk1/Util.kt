package com.example.fitboymk1

import android.os.Bundle


fun bundle2string(bundle: Bundle?): String? {
    if (bundle == null) {
        return null
    }
    var string = "Bundle{"
    for (key in bundle.keySet()) {
        string += " " + key + " => " + bundle[key] + ";"
    }
    string += " }Bundle"
    return string
}
