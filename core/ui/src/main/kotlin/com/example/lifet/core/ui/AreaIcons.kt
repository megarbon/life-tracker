package com.example.lifet.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.lifet.core.model.Area

/** Icono de cada área; único lugar donde se traduce `Area` a icono. */
public val Area.icon: ImageVector
    get() =
        when (this) {
            Area.FINANCE -> Icons.Rounded.AccountBalanceWallet
            Area.NUTRITION -> Icons.Rounded.Restaurant
            Area.FITNESS -> Icons.Rounded.FitnessCenter
            Area.QUIT -> Icons.Rounded.SmokeFree
            Area.WORK -> Icons.Rounded.Work
            Area.MOOD -> Icons.Rounded.SentimentSatisfied
        }
