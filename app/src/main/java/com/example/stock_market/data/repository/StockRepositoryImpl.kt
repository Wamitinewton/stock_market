package com.example.stock_market.data.repository

import com.example.stock_market.data.csv.CsvParser
import com.example.stock_market.data.local.StockDatabase
import com.example.stock_market.data.mapper.toCompanyListing
import com.example.stock_market.data.mapper.toCompanyListingEntity
import com.example.stock_market.data.remote.StockApi
import com.example.stock_market.domain.model.CompanyListing
import com.example.stock_market.domain.repository.StockRepository
import com.example.stock_market.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

// this is a concrete implementation
// of an abstraction
@Singleton
class StockRepositoryImpl @Inject constructor(
   private val api: StockApi,
   private val db: StockDatabase,
  private  val companyListingParser: CsvParser<CompanyListing>
) : StockRepository {
    private val dao = db.dao

    override suspend fun getCompanyListings(
        fetchRemote: Boolean,
        query: String
    ): Flow<Resource<List<CompanyListing>>> {
        return flow {
            emit(Resource.Loading(true))
            val localListings = dao.searchCompanyListings(query)
            emit(Resource.Success(
                data = localListings.map { it.toCompanyListing() }
            ))
            val isDbEmpty = localListings.isEmpty() && query.isBlank()
            val shouldLoadFromCache = !isDbEmpty && !fetchRemote

            if (shouldLoadFromCache) {
                emit(Resource.Loading(false))
                return@flow
            }
            val remoteListings = try {
                val response = api.getListings()
                companyListingParser.parse(response.byteStream())
            } catch (e: IOException) {
                e.printStackTrace()
                emit(Resource.Error("couldn't load data from api"))
                null
            } catch (e: HttpException) {
                e.printStackTrace()
                emit(Resource.Error("couldn't load data"))
                null
            }
            remoteListings?.let { listings ->
                dao.clearCompanyListings()
                dao.insertCompanyListings(
                    listings.map { it.toCompanyListingEntity() }
                )
                emit(Resource.Success(
                    data = dao.searchCompanyListings("")
                        .map { it.toCompanyListing() }
                ))
                emit(Resource.Loading(false))
            }
        }
    }
}