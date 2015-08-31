package net.thucydides.core.reports.json;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.model.ReportType;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.reports.AcceptanceTestLoader;
import net.thucydides.core.reports.AcceptanceTestReporter;
import net.thucydides.core.reports.OutcomeFormat;
import net.thucydides.core.reports.TestOutcomes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Locale;
import java.lang.RuntimeException;

public class JSONTestOutcomeReporter implements AcceptanceTestReporter, AcceptanceTestLoader {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(JSONTestOutcomeReporter.class);

    private File outputDirectory;

    private transient String qualifier;

    @Override
    public String getName() {
        return "json";
    }

    JSONConverter jsonConverter;

    public JSONTestOutcomeReporter() {
        jsonConverter = Injectors.getInjector().getInstance(JSONConverter.class);
    }

    @Override
    public File generateReportFor(TestOutcome testOutcome,
                                  TestOutcomes allTestOutcomes) throws IOException {
        TestOutcome storedTestOutcome = testOutcome.withQualifier(qualifier);
        Preconditions.checkNotNull(outputDirectory);
        String reportFilename = reportFor(storedTestOutcome);
        File report = new File(getOutputDirectory(), reportFilename);
        try(OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(report))){
            jsonConverter.toJson(storedTestOutcome, outputStream);
        }
        return report;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    private String reportFor(final TestOutcome testOutcome) {
        return testOutcome.withQualifier(qualifier).getReportName(
                ReportType.JSON);
    }

    public void setOutputDirectory(final File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void setQualifier(final String qualifier) {
        this.qualifier = qualifier;
    }

    public void setResourceDirectory(String resourceDirectoryPath) {
    }

    public Optional<TestOutcome> loadReportFrom(final File reportFile) {
        try(BufferedInputStream report = new BufferedInputStream(new FileInputStream(reportFile))) {
            TestOutcome fromJson = jsonConverter.fromJson(report);
            return Optional.fromNullable(fromJson);
        } catch (Throwable e) {
            LOGGER.warn("File was not a valid JSON Serenity test report: " + reportFile.getName()
                        + System.lineSeparator() + "File path: " + reportFile.getAbsolutePath(), e);
            throw new RuntimeException("Unable to read JSON from test report.", e);
        }
    }

    @Override
    public List<TestOutcome> loadReportsFrom(File outputDirectory) {
        File[] reportFiles = getAllJsonFilesFrom(outputDirectory);
        List<TestOutcome> testOutcomes = Lists.newArrayList();
        if (reportFiles != null) {
            for (File reportFile : reportFiles) {
                testOutcomes.addAll(loadReportFrom(reportFile).asSet());
            }
        }
        return testOutcomes;
    }

    private File[] getAllJsonFilesFrom(final File reportsDirectory) {
        return reportsDirectory.listFiles(new JsonFilenameFilter());
    }

    private static final class JsonFilenameFilter implements FilenameFilter {
        public boolean accept(final File file, final String filename) {
            return filename.toLowerCase(Locale.getDefault()).endsWith(".json");
        }
    }

    @Override
    public Optional<OutcomeFormat> getFormat() {
        return Optional.of(OutcomeFormat.JSON);
    }
}
