package io.github.numq.grokviewer.content

import io.github.numq.grokviewer.usecase.UseCase

class SaveContent(private val service: ContentService) : UseCase<SaveContent.Input, Unit> {
    data class Input(val content: Content, val path: String, val name: String)

    override suspend fun execute(input: Input) = with(input) {
        service.save(content = content, path = path, name = name)
    }
}