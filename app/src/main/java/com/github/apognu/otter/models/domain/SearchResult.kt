package com.github.apognu.otter.models.domain

interface SearchResult {
  fun cover(): String?
  fun title(): String
  fun subtitle(): String
}
