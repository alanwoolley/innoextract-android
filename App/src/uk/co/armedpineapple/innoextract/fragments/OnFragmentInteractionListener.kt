package uk.co.armedpineapple.innoextract.fragments

/**
 * Various callbacks from the key application fragments.
 */
interface OnFragmentInteractionListener {

    /**
     * Invoked when the extract button has been pressed.
     * At this point, a valid Inno Setup source and target
     * directory should have been selected and configured.
     */
    fun onExtractButtonPressed()

    /**
     * Invoked when a source has been selected. At this point, the source may not
     * represent a valid Inno Setup file.
     */
    fun onFileSelected()

    /**
     * Invoked when the return button has been pressed after extracting.
     */
    fun onReturnButtonPressed()
}