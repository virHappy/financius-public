package com.code44.finance.ui.currencies;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.code44.finance.R;
import com.code44.finance.adapters.BaseModelsAdapter;
import com.code44.finance.adapters.CurrenciesAdapter;
import com.code44.finance.adapters.CurrenciesAdapterV2;
import com.code44.finance.api.currencies.CurrenciesAsyncApi;
import com.code44.finance.api.currencies.CurrencyRequest;
import com.code44.finance.data.Query;
import com.code44.finance.data.db.Tables;
import com.code44.finance.data.db.model.BaseModel;
import com.code44.finance.data.db.model.Currency;
import com.code44.finance.data.providers.CurrenciesProvider;
import com.code44.finance.ui.ModelListFragment;
import com.code44.finance.utils.GeneralPrefs;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class CurrenciesFragment extends ModelListFragment implements CompoundButton.OnCheckedChangeListener {
    private final List<Currency> currencies = new ArrayList<>();

    private RecyclerView recycler_V;
    private SmoothProgressBar loading_SPB;

    private CurrenciesAdapterV2 adapterV2;
    private RecyclerView.LayoutManager layoutManager;

    public static CurrenciesFragment newInstance(int mode) {
        final Bundle args = makeArgs(mode);

        final CurrenciesFragment fragment = new CurrenciesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_currencies, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get views
        recycler_V = (RecyclerView) view.findViewById(R.id.recycler_V);
        loading_SPB = (SmoothProgressBar) view.findViewById(R.id.loading_SPB);
        final View separator_V = view.findViewById(R.id.separator_V);
        final View container_V = view.findViewById(R.id.container_V);
        final Switch autoUpdateCurrencies_S = (Switch) view.findViewById(R.id.autoUpdateCurrencies_S);

        // Setup
        layoutManager = new LinearLayoutManager(getActivity());
        adapterV2 = new CurrenciesAdapterV2();
        recycler_V.setHasFixedSize(true);
        recycler_V.setLayoutManager(layoutManager);
        recycler_V.setAdapter(adapterV2);
        view.findViewById(R.id.list_V).setVisibility(View.GONE);
        autoUpdateCurrencies_S.setChecked(GeneralPrefs.get().isAutoUpdateCurrencies());
        autoUpdateCurrencies_S.setOnCheckedChangeListener(this);
        if (isSelectMode()) {
            separator_V.setVisibility(View.GONE);
            container_V.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRefreshView();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.currencies, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh_rates:
                refreshRates();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void startModelActivity(Context context, View expandFrom, long modelId) {
        CurrencyActivity.start(context, modelId);
    }

    @Override
    protected void startModelEditActivity(Context context, View expandFrom, long modelId) {
        CurrencyEditActivity.start(context, expandFrom, modelId);
    }

    @Override
    protected BaseModelsAdapter createAdapter(Context context) {
        return new CurrenciesAdapter(context);
    }

    @Override
    protected Uri getUri() {
        return CurrenciesProvider.uriCurrencies();
    }

    @Override
    protected BaseModel modelFrom(Cursor cursor) {
        return Currency.from(cursor);
    }

    @Override
    protected Query getQuery() {
        return Query.get()
                .projectionId(Tables.Currencies.ID)
                .projection(Tables.Currencies.PROJECTION)
                .sortOrder(Tables.Currencies.IS_DEFAULT + " desc")
                .sortOrder(Tables.Currencies.CODE.getName());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_MODELS) {
            currencies.clear();
            if (data.moveToFirst()) {
                do {
                    currencies.add(Currency.from(data));
                } while (data.moveToNext());
            }
        }
        adapterV2.swapCursor(data);
        //super.onLoadFinished(loader, data);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        GeneralPrefs.get().setAutoUpdateCurrencies(isChecked);
        if (isChecked) {
            refreshRates();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CurrencyRequest.CurrencyRequestEvent event) {
        updateRefreshView();
    }

    private void refreshRates() {
        for (Currency currency : currencies) {
            if (!currency.isDefault()) {
                CurrenciesAsyncApi.get().updateExchangeRate(currency.getCode());
            }
        }
    }

    private void updateRefreshView() {
        final boolean isFetchingCurrencies = EventBus.getDefault().getStickyEvent(CurrencyRequest.CurrencyRequestEvent.class) != null;
        loading_SPB.setVisibility(isFetchingCurrencies ? View.VISIBLE : View.GONE);
    }
}