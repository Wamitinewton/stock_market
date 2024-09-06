package com.example.stock_market.domain.repository

import com.example.stock_market.domain.model.CompanyListing
import com.example.stock_market.utils.Resource
import kotlinx.coroutines.flow.Flow

interface StockRepository {

    suspend fun getCompanyListings(
        fetchRemote: Boolean,
        query: String
    ): Flow<Resource<List<CompanyListing>>>
}