package it.polito.mad.carpooling_09.utils

interface DrawerLocker {
    /**
     * Hide or show the NavDrawer from the view.
     *
     * @param shouldLock true for hiding, false for showing
     */
    fun setDrawerLocked(shouldLock: Boolean)
}