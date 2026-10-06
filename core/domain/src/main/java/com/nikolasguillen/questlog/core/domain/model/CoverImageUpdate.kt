package com.nikolasguillen.questlog.core.domain.model

/**
 * What to do with a wishlist's cover image when its details are edited.
 */
sealed interface CoverImageUpdate {
    /** The user did not touch the cover: the stored image stays as it is. */
    data object Keep : CoverImageUpdate

    /** The user removed the cover: the list ends up with none and the old file is deleted. */
    data object Remove : CoverImageUpdate

    /**
     * The user picked a new image.
     *
     * @property sourceUri Content URI of the picked image, still to be copied into app storage.
     */
    data class Replace(val sourceUri: String) : CoverImageUpdate
}
