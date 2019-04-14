package org.blendee.cli;

import org.blendee.jdbc.Metadata;
import org.blendee.jdbc.Metadatas;
import org.blendee.jdbc.impl.JDBCMetadata;
import org.blendee.util.AnnotationMetadataFactory;

public class CLIAnnotationMetadataFactory extends AnnotationMetadataFactory {

	@Override
	public Metadata createMetadata() {
		return new Metadatas(new JDBCMetadata(), super.createMetadata());
	}

	@Override
	protected Metadata getDepends() {
		return new JDBCMetadata();
	}

	/**
	 * DB からカラムの最新状態を取り込みなおす
	 */
	@Override
	protected boolean usesAllVirtualColumns() {
		return false;
	}
}
