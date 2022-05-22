package com.monta.changelog.printer

import com.monta.changelog.model.ChangeLog
import com.monta.changelog.util.LinkResolver

interface ChangeLogPrinter {
    suspend fun print(linkResolvers: List<LinkResolver>, changeLog: ChangeLog)
}
