package com.filyar.app;

import com.filyar.app.RemoteFile;

interface IFileService {
    void destroy() = 16777114;

    List<RemoteFile> listRange(String path, int offset, int limit) = 1;
    RemoteFile stat(String path) = 2;
    boolean mkdirs(String path) = 3;
    boolean createFile(String path) = 4;
    boolean delete(String path) = 5;
    boolean rename(String from, String to) = 6;
    ParcelFileDescriptor open(String path, String mode) = 7;
    long[] storage(String path) = 8;
    List<RemoteFile> search(String root, String query, int limit) = 9;
    long[] measure(String path) = 10;
    boolean setModified(String path, long time) = 11;
    List<RemoteFile> scanRange(String root, long minSize, int offset, int limit) = 12;
}
