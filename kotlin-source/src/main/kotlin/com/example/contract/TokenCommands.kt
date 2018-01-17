package com.example.contract

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.TypeOnlyCommandData

interface TokenCommands : CommandData {
    class Create : TypeOnlyCommandData(), TokenCommands
    class Move : TypeOnlyCommandData(), TokenCommands
    class Burn : TypeOnlyCommandData(), TokenCommands
}

