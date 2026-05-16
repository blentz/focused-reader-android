package com.focusedreader.capture

import android.service.quicksettings.TileService
import android.widget.Toast

class QuickCaptureTileService : TileService() {
    override fun onClick() {
        val svc = FocusedReaderA11yService.instance
        if (svc == null) {
            Toast.makeText(this, "Enable Focused Reader accessibility service first", Toast.LENGTH_LONG).show()
        } else {
            svc.requestCapture()
            Toast.makeText(this, "Captured", Toast.LENGTH_SHORT).show()
        }
    }
}
