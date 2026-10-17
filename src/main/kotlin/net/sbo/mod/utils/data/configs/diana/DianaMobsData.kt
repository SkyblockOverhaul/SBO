package net.sbo.mod.utils.data.configs.diana

import com.google.gson.annotations.SerializedName

@Suppress("PropertyName")
data class DianaMobsData(
    @SerializedName("King Minos") var KING_MINOS: Int = 0,
    @SerializedName("Manticore") var MANTICORE: Int = 0,
    @SerializedName("Minos Inquisitor") var MINOS_INQUISITOR: Int = 0,
    @SerializedName("Sphinx") var SPHINX: Int = 0,
    @SerializedName("Minos Champion") var MINOS_CHAMPION: Int = 0,
    @SerializedName("Minotaur") var MINOTAUR: Int = 0,
    @SerializedName("Gaia Construct") var GAIA_CONSTRUCT: Int = 0,
    @SerializedName("Harpy") var HARPY: Int = 0,
    @SerializedName("Cretan Bull") var CRETAN_BULL: Int = 0,
    @SerializedName("Stranded Nymph") var STRANDED_NYMPH: Int = 0,
    @SerializedName("Siamese Lynxes") var SIAMESE_LYNXES: Int = 0,
    @SerializedName("Minos Hunter") var MINOS_HUNTER: Int = 0,
    @SerializedName("TotalMobs") var TOTAL_MOBS: Int = 0,
    @SerializedName("Minos Inquisitor Ls") var MINOS_INQUISITOR_LS: Int = 0,
    @SerializedName("King Minos Ls") var KING_MINOS_LS: Int = 0,
    @SerializedName("Manticore Ls") var MANTICORE_LS: Int = 0,
    @SerializedName("Sphinx Ls") var SPHINX_LS: Int = 0,
)
