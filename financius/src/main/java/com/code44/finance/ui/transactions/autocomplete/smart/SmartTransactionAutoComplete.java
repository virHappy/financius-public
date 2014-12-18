package com.code44.finance.ui.transactions.autocomplete.smart;

import android.content.Context;
import android.support.v4.util.Pair;

import com.code44.finance.data.model.Category;
import com.code44.finance.data.model.Tag;
import com.code44.finance.ui.transactions.autocomplete.AutoCompleteInput;
import com.code44.finance.ui.transactions.autocomplete.AutoCompleteResult;
import com.code44.finance.ui.transactions.autocomplete.TransactionAutoComplete;

import java.util.List;
import java.util.concurrent.Executor;

public class SmartTransactionAutoComplete extends TransactionAutoComplete {
    private final Context context;
    private final boolean log;

    public SmartTransactionAutoComplete(Context context, Executor executor, TransactionAutoCompleteListener listener, AutoCompleteInput autoCompleteInput, boolean log) {
        super(executor, listener, autoCompleteInput);
        this.context = context.getApplicationContext();
        this.log = log;
    }

    @Override protected AutoCompleteResult autoComplete(AutoCompleteInput input) {
        final AutoCompleteResult result = new AutoCompleteResult();
        final Pair<Category, List<Category>> categoriesResult = new CategoriesFinder(context, input, log).find();
        final Pair<List<Tag>, List<List<Tag>>> tagsResult = new TagsFinder(context, input, log, categoriesResult.first).find();

        result.setCategory(categoriesResult.first);
        result.setOtherCategories(categoriesResult.second);

        result.setTags(tagsResult.first);
        // TODO Set other tags

        return result;
    }
}