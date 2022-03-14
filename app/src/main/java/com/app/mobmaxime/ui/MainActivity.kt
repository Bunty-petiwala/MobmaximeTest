package com.app.mobmaxime.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.app.mobmaxime.R
import com.app.mobmaxime.data.WeatherInfoShowModel
import com.app.mobmaxime.data.WeatherInfoShowModelImpl
import com.app.mobmaxime.databinding.*
import com.app.mobmaxime.ui.viewmodel.WeatherInfoViewModel
import com.app.mobmaxime.utils.convertToListOfCityName
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hellohasan.weatherappmvvm.features.weather_info_show.model.data_class.City
import com.hellohasan.weatherappmvvm.features.weather_info_show.model.data_class.WeatherData

class MainActivity : AppCompatActivity() {

    private lateinit var model: WeatherInfoShowModel
    private lateinit var viewModel: WeatherInfoViewModel

    private var cityList: MutableList<City> = mutableListOf()

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mInputBinding: LayoutInputPartBinding
    private lateinit var mSunBinding: LayoutSunriseSunsetBinding
    private lateinit var mAdditionalBinding: LayoutWeatherAdditionalInfoBinding
    private lateinit var mBasicBinding: LayoutWeatherBasicInfoBinding

    private lateinit var mPreference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        mInputBinding = LayoutInputPartBinding.bind(mBinding.root)
        mSunBinding = LayoutSunriseSunsetBinding.bind(mBinding.root)
        mAdditionalBinding = LayoutWeatherAdditionalInfoBinding.bind(mBinding.root)
        mBasicBinding = LayoutWeatherBasicInfoBinding.bind(mBinding.root)
        setContentView(mBinding.root)

        mPreference = getSharedPreferences(LoginActivity.My_Pref, MODE_PRIVATE)

        // initialize model. (I know we should not initialize model in View. But for simplicity...)
        model = WeatherInfoShowModelImpl(applicationContext)
        // initialize ViewModel
        viewModel = ViewModelProviders.of(this).get(WeatherInfoViewModel::class.java)

        // set LiveData and View click listeners before the call for data fetching
        setLiveDataListeners()
        setViewClickListener()

        /**
         * Fetch city list when Activity open.
         * It's not a very good way that, passing model in every methods of ViewModel. For the sake
         * of simplicity I did so. In real production level App, we can inject out model to ViewModel
         * as a parameter by any dependency injection library like Dagger.
         */
        viewModel.getCityList(model)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.logout -> logoutClick()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setViewClickListener() {
        // View Weather button click listener

        mInputBinding.btnViewWeather.setOnClickListener {
            val selectedCityId = cityList[mInputBinding.spinner.selectedItemPosition].id
            viewModel.getWeatherInfo(selectedCityId, model) // fetch weather info
        }
    }

    private fun setLiveDataListeners() {

        /**
         * When ViewModel PUSH city list to LiveData then this `onChanged()`‍ method will be called.
         * Here we subscribe the LiveData of City list. We don't pull city list from ViewModel.
         * We subscribe to the data source for city list. When LiveData of city list is updated
         * inside ViewModel, below onChanged() method will triggered instantly.
         * City list is fetching from a small local JSON file. So we don't need any ProgressBar here.
         *
         * For better understanding, I didn't use lambda in this method call. Rather thant lambda I
         * implement `Observer` interface in general format. Hope you will understand the inline
         * implementation of `Observer` interface. Rest of the `observe()` method, I've used lambda
         * to short the code.
         */
        viewModel.cityListLiveData.observe(this, object : Observer<MutableList<City>> {
            override fun onChanged(cities: MutableList<City>) {
                setCityListSpinner(cities)
            }
        })

        /**
         * If ViewModel failed to fetch City list from data source, this LiveData will be triggered.
         * I know it's not good to make separate LiveData both for Success and Failure, but for sake
         * of simplification I did it. We can handle all of our errors from our Activity or Fragment
         * Base classes. Another way is: using a Generic wrapper class where you can set the success
         * or failure status for any types of data model.
         *
         * Here I've used lambda expression to implement Observer interface in second parameter.
         */
        viewModel.cityListFailureLiveData.observe(this, Observer { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        })

        /**
         * ProgressBar visibility will be handled by this LiveData. ViewModel decides when Activity
         * should show ProgressBar and when hide.
         *
         * Here I've used lambda expression to implement Observer interface in second parameter.
         */
        viewModel.progressBarLiveData.observe(this, Observer { isShowLoader ->
            if (isShowLoader)
                mBinding.progressBar.visibility = View.VISIBLE
            else
                mBinding.progressBar.visibility = View.GONE
        })

        /**
         * This method will be triggered when ViewModel successfully receive WeatherData from our
         * data source (I mean Model). Activity just observing (subscribing) this LiveData for showing
         * weather information on UI. ViewModel receives Weather data API response from Model via
         * Callback method of Model. Then ViewModel apply some business logic and manipulate data.
         * Finally ViewModel PUSH WeatherData to `weatherInfoLiveData`. After PUSHING into it, below
         * method triggered instantly! Then we set the data on UI.
         *
         * Here I've used lambda expression to implement Observer interface in second parameter.
         */
        viewModel.weatherInfoLiveData.observe(this, Observer { weatherData ->
            setWeatherInfo(weatherData)
        })

        /**
         * If ViewModel faces any error during Weather Info fetching API call by Model, then PUSH the
         * error message into `weatherInfoFailureLiveData`. After that, this method will be triggered.
         * Then we will hide the output view and show error message on UI.
         *
         * Here I've used lambda expression to implement Observer interface in second parameter.
         */
        viewModel.weatherInfoFailureLiveData.observe(this, Observer { errorMessage ->
            mBinding.outputGroup.visibility = View.GONE
            mBinding.tvErrorMessage.visibility = View.VISIBLE
            mBinding.tvErrorMessage.text = errorMessage
        })
    }

    private fun setCityListSpinner(cityList: MutableList<City>) {
        this.cityList = cityList

        val arrayAdapter = ArrayAdapter(
            this,
            R.layout.support_simple_spinner_dropdown_item,
            this.cityList.convertToListOfCityName()
        )

        mInputBinding.spinner.adapter = arrayAdapter
    }

    private fun setWeatherInfo(weatherData: WeatherData) {
        mBinding.outputGroup.visibility = View.VISIBLE
        mBinding.tvErrorMessage.visibility = View.GONE

        mBasicBinding.tvDateTime.text = weatherData.dateTime
        mBasicBinding.tvTemperature.text = weatherData.temperature
        mBasicBinding.tvCityCountry.text = weatherData.cityAndCountry
        Glide.with(this).load(weatherData.weatherConditionIconUrl).into(mBasicBinding.ivWeatherCondition)
        mBasicBinding.tvWeatherCondition.text = weatherData.weatherConditionIconDescription

        mAdditionalBinding.tvHumidityValue.text = weatherData.humidity
        mAdditionalBinding.tvPressureValue.text = weatherData.pressure
        mAdditionalBinding.tvVisibilityValue.text = weatherData.visibility

        mSunBinding.tvSunriseTime.text = weatherData.sunrise
        mSunBinding.tvSunsetTime.text = weatherData.sunset
    }

    private fun logoutClick(){
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton(
                "Yes"
            ) { dialogInterface, i ->
                signout()
                mPreference.edit().putBoolean(LoginActivity.IsLogin, false).apply()
                finish()
                dialogInterface.dismiss()
            }
            .setNegativeButton(
                "No"
            ) { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun getGoogleSingInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(this, gso);
    }

    private fun isUserSignedIn(): Boolean {

        val account = GoogleSignIn.getLastSignedInAccount(this)
        return account != null

    }

    private fun signout() {
        if (isUserSignedIn()) {
            getGoogleSingInClient().signOut().addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this@MainActivity, " Signed out ", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, " Error ", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}