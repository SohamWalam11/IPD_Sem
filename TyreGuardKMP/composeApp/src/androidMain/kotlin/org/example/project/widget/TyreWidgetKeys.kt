package org.example.project.widget

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object TyreWidgetKeys {
    val PSI_FL = floatPreferencesKey("psi_fl")
    val PSI_FR = floatPreferencesKey("psi_fr")
    val PSI_RL = floatPreferencesKey("psi_rl")
    val PSI_RR = floatPreferencesKey("psi_rr")
    val LAST_UPDATED = stringPreferencesKey("last_updated")
}
