package io.github.numq.grokviewer.usecase

interface UseCase<in Input, out Output> {
    suspend fun execute(input: Input): Result<Output>
}