/*
 * Minecraft Forge
 * Copyright (c) 2016-2022.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.loading;

import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraftforge.fml.loading.moddiscovery.BackgroundScanHandler;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Master list of all mods <em>in the loading context. This class cannot refer outside the
 * loading package</em>
 */
public class LoadingModList
{
    private static LoadingModList INSTANCE;
    private final List<ModFileInfo> modFiles;
    private final List<ModInfo> sortedList;
    private final Map<String, ModFileInfo> fileById;
    @SuppressWarnings("unused")
    private BackgroundScanHandler scanner;
    private final List<EarlyLoadingException> preLoadErrors;
    private List<ModFile> brokenFiles;

    private LoadingModList(final List<ModFile> modFiles, final List<ModInfo> sortedList)
    {
        this.modFiles = modFiles.stream()
                .map(ModFile::getModFileInfo)
                .map(ModFileInfo.class::cast)
                .collect(Collectors.toList());
        this.sortedList = sortedList.stream()
                .map(ModInfo.class::cast)
                .collect(Collectors.toList());
        this.fileById = this.modFiles.stream()
                .map(ModFileInfo::getMods)
                .flatMap(Collection::stream)
                .map(ModInfo.class::cast)
                .collect(Collectors.toMap(ModInfo::getModId, ModInfo::getOwningFile));
        this.preLoadErrors = new ArrayList<>();
    }

    public static LoadingModList of(List<ModFile> modFiles, List<ModInfo> sortedList, final EarlyLoadingException earlyLoadingException)
    {
        INSTANCE = new LoadingModList(modFiles, sortedList);
        if (earlyLoadingException != null)
        {
            INSTANCE.preLoadErrors.add(earlyLoadingException);
        }
        return INSTANCE;
    }

    public static LoadingModList get() {
        return INSTANCE;
    }
    public void addCoreMods()
    {
        modFiles.stream()
                .map(ModFileInfo::getFile)
                .map(ModFile::getCoreMods)
                .flatMap(List::stream)
                .forEach(FMLLoader.getCoreModProvider()::addCoreMod);
    }

    public void addAccessTransformers()
    {
        modFiles.stream()
                .map(ModFileInfo::getFile)
                .forEach(mod -> mod.getAccessTransformer().ifPresent(path -> FMLLoader.addAccessTransformer(path, mod)));
    }

    public void addForScanning(BackgroundScanHandler backgroundScanHandler)
    {
        this.scanner = backgroundScanHandler;
        backgroundScanHandler.setLoadingModList(this);
        modFiles.stream()
                .map(ModFileInfo::getFile)
                .forEach(backgroundScanHandler::submitForScanning);
    }

    public List<ModFileInfo> getModFiles()
    {
        return modFiles;
    }

    public Path findResource(final String className)
    {
        for (ModFileInfo mf : modFiles) {
            final Path resource = mf.getFile().findResource(className);
            if (Files.exists(resource)) return resource;
        }
        return null;
    }

    public Enumeration<URL> findAllURLsForResource(final String resName) {
        final String resourceName;
        // strip a leading slash
        if (resName.startsWith("/")) {
            resourceName = resName.substring(1);
        } else {
            resourceName = resName;
        }
        return new Enumeration<URL>() {
            private final Iterator<ModFileInfo> modFileIterator = modFiles.iterator();
            private URL next;
            @Override
            public boolean hasMoreElements() {
                if (next!=null) return true;
                next = findNextURL();
                return next != null;
            }

            @Override
            public URL nextElement() {
                if (next == null) {
                    next = findNextURL();
                    if (next == null) throw new NoSuchElementException();
                }
                URL result = next;
                next = null;
                return result;
            }

            private URL findNextURL() {
                while (modFileIterator.hasNext()) {
                    final ModFileInfo next = modFileIterator.next();
                    final Path resource = next.getFile().findResource(resourceName);
                    if (Files.exists(resource)) {
                        return LamdbaExceptionUtils.uncheck(()->new URL("modjar://" + next.getMods().get(0).getModId() + "/" + resourceName));
                    }
                }
                return null;
            }
        };
    }

    public ModFileInfo getModFileById(String modid)
    {
        return this.fileById.get(modid);
    }

    public List<ModInfo> getMods()
    {
        return this.sortedList;
    }

    public List<EarlyLoadingException> getErrors() {
        return preLoadErrors;
    }

    public void setBrokenFiles(final List<ModFile> brokenFiles) {
        this.brokenFiles = brokenFiles;
    }

    public List<ModFile> getBrokenFiles() {
        return this.brokenFiles;
    }
}
