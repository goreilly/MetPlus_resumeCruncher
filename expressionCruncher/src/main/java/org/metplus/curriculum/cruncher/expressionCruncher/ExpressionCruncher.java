package org.metplus.curriculum.cruncher.expressionCruncher;

import org.metplus.curriculum.cruncher.Cruncher;
import org.metplus.curriculum.cruncher.Matcher;
import org.metplus.curriculum.database.domain.*;
import org.metplus.curriculum.database.exceptions.CruncherSettingsNotFound;
import org.metplus.curriculum.database.repository.JobRepository;
import org.metplus.curriculum.database.repository.ResumeRepository;
import org.metplus.curriculum.database.repository.SettingsRepository;
import org.metplus.curriculum.init.CruncherInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Joao Pereira on 31/08/2015.
 */
@Component
@ConfigurationProperties(locations = "classpath:expressionCruncher.yml", prefix = "config")
public class ExpressionCruncher extends CruncherInitializer {
    private static final String IGNORE_LIST = "IgnoreList";
    private static final String MERGE_LIST = "MergeList";
    private static final String CASE_SENSITIVE = "CaseSensitive";
    private static final String IGNORE_LIST_WORD = "IgnoreListWordSearch";

    private boolean caseSensitive;
    private Map<String, List<String>> mergeList;
    private List<String> ignoreList;
    private boolean ignoreListWordSearch;

    public boolean isIgnoreListWordSearch() {
        return ignoreListWordSearch;
    }

    public void setIgnoreListWordSearch(boolean ignoreListWordSearch) {
        this.ignoreListWordSearch = ignoreListWordSearch;
    }


    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public Map<String, List<String>> getMergeList() {
        return mergeList;
    }

    public void setMergeList(Map<String, List<String>> mergeList) {
        this.mergeList = mergeList;
    }

    public List<String> getIgnoreList() {
        return ignoreList;
    }

    public void setIgnoreList(List<String> ignoreList) {
        this.ignoreList = ignoreList;
    }



    private CruncherImpl cruncherImpl;
    private MatcherImpl resumeMatcher;
    @Autowired
    private SettingsRepository repository;
    @Autowired
    private ResumeRepository resumeRepository;
    @Autowired
    private JobRepository jobRepository;

    @Override
    public void init() {
        try {
            CruncherSettings settings = null;
            try {
                settings = repository.findAll().iterator().next().getCruncherSettings(CruncherImpl.CRUNCHER_NAME);
            } catch(NoSuchElementException e) {
                repository.save(new Settings());
                settings = repository.findAll().iterator().next().getCruncherSettings(CruncherImpl.CRUNCHER_NAME);
            }
            ignoreList = (List)settings.getSetting(IGNORE_LIST).getData();
            caseSensitive = (boolean)settings.getSetting(CASE_SENSITIVE).getData();
            mergeList = (Map) settings.getSetting(MERGE_LIST).getData();
            ignoreListWordSearch = (boolean) settings.getSetting(IGNORE_LIST_WORD).getData();
            System.out.println("===================================================================");
            System.out.println("Case sensitive: " + caseSensitive);
            System.out.println("Ignore list: " + ignoreList);
            System.out.println("Ignore List Search Word: " + ignoreListWordSearch);
            System.out.println("Merge List: " + mergeList);
            System.out.println("===================================================================");

            cruncherImpl = new CruncherImpl(ignoreList, mergeList);
            cruncherImpl.setCaseSensitive(caseSensitive);
            cruncherImpl.setIgnoreListSearchWord(ignoreListWordSearch);
        } catch (CruncherSettingsNotFound cruncherSettingsNotFound) {
            System.out.println("===================================================================");
            System.out.println("Case sensitive: " + caseSensitive);
            System.out.println("Ignore list: " + ignoreList);
            System.out.println("Ignore List Search Word: " + ignoreListWordSearch);
            System.out.println("Merge List: " + mergeList);
            System.out.println("===================================================================");
            cruncherImpl = new CruncherImpl(ignoreList, mergeList);
            cruncherImpl.setCaseSensitive(caseSensitive);
            cruncherImpl.setIgnoreListSearchWord(ignoreListWordSearch);
            save();
        }
        resumeMatcher = new MatcherImpl(cruncherImpl, resumeRepository, jobRepository);
    }

    /**
     * Function used to retrieve the cruncher
     *
     * @return Cruncher to be used
     */
    @Override
    public Cruncher getCruncher() {
        return cruncherImpl;
    }

    /**
     * Save the settings of the cruncher to the database
     */
    public void save() {
        CruncherSettings cSettings;
        try {
            cSettings = repository.findAll().iterator().next().getCruncherSettings(CruncherImpl.CRUNCHER_NAME);
        } catch (CruncherSettingsNotFound cruncherSettingsNotFound) {
            cSettings = new CruncherSettings(CruncherImpl.CRUNCHER_NAME);
        }
        cSettings.addSetting(new Setting<>(CASE_SENSITIVE, cruncherImpl.isCaseSensitive()));
        cSettings.addSetting(new Setting<>(IGNORE_LIST, cruncherImpl.getIgnoreList()));
        cSettings.addSetting(new Setting<>(IGNORE_LIST_WORD, cruncherImpl.isIgnoreListSearchWord()));
        cSettings.addSetting(new Setting<>(MERGE_LIST, cruncherImpl.getMergeList()));
        Settings globalSettings = repository.findAll().iterator().next();
        globalSettings.addCruncherSettings(CruncherImpl.CRUNCHER_NAME, cSettings);
        repository.save(globalSettings);
    }

    @Override
    public Matcher getMatcher() {
        return resumeMatcher;
    }
}
