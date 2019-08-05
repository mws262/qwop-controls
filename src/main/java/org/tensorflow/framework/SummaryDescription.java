// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: tensorflow/core/framework/summary.proto

package org.tensorflow.framework;

/**
 * <pre>
 * Metadata associated with a series of Summary data
 * </pre>
 *
 * Protobuf type {@code tensorflow.SummaryDescription}
 */
public  final class SummaryDescription extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:tensorflow.SummaryDescription)
    SummaryDescriptionOrBuilder {
  // Use SummaryDescription.newBuilder() to construct.
  private SummaryDescription(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private SummaryDescription() {
    typeHint_ = "";
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
  }
  private SummaryDescription(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    int mutable_bitField0_ = 0;
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!input.skipField(tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            java.lang.String s = input.readStringRequireUtf8();

            typeHint_ = s;
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return org.tensorflow.framework.SummaryProtos.internal_static_tensorflow_SummaryDescription_descriptor;
  }

  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return org.tensorflow.framework.SummaryProtos.internal_static_tensorflow_SummaryDescription_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            org.tensorflow.framework.SummaryDescription.class, org.tensorflow.framework.SummaryDescription.Builder.class);
  }

  public static final int TYPE_HINT_FIELD_NUMBER = 1;
  private volatile java.lang.Object typeHint_;
  /**
   * <pre>
   * Hint on how plugins should process the data in this series.
   * Supported values include "scalar", "histogram", "image", "audio"
   * </pre>
   *
   * <code>optional string type_hint = 1;</code>
   */
  public java.lang.String getTypeHint() {
    java.lang.Object ref = typeHint_;
    if (ref instanceof java.lang.String) {
      return (java.lang.String) ref;
    } else {
      com.google.protobuf.ByteString bs = 
          (com.google.protobuf.ByteString) ref;
      java.lang.String s = bs.toStringUtf8();
      typeHint_ = s;
      return s;
    }
  }
  /**
   * <pre>
   * Hint on how plugins should process the data in this series.
   * Supported values include "scalar", "histogram", "image", "audio"
   * </pre>
   *
   * <code>optional string type_hint = 1;</code>
   */
  public com.google.protobuf.ByteString
      getTypeHintBytes() {
    java.lang.Object ref = typeHint_;
    if (ref instanceof java.lang.String) {
      com.google.protobuf.ByteString b = 
          com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
      typeHint_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!getTypeHintBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, typeHint_);
    }
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!getTypeHintBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, typeHint_);
    }
    memoizedSize = size;
    return size;
  }

  private static final long serialVersionUID = 0L;
  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof org.tensorflow.framework.SummaryDescription)) {
      return super.equals(obj);
    }
    org.tensorflow.framework.SummaryDescription other = (org.tensorflow.framework.SummaryDescription) obj;

    boolean result = true;
    result = result && getTypeHint()
        .equals(other.getTypeHint());
    return result;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptorForType().hashCode();
    hash = (37 * hash) + TYPE_HINT_FIELD_NUMBER;
    hash = (53 * hash) + getTypeHint().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static org.tensorflow.framework.SummaryDescription parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.tensorflow.framework.SummaryDescription parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.tensorflow.framework.SummaryDescription parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static org.tensorflow.framework.SummaryDescription parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static org.tensorflow.framework.SummaryDescription parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.tensorflow.framework.SummaryDescription parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.tensorflow.framework.SummaryDescription parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static org.tensorflow.framework.SummaryDescription parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static org.tensorflow.framework.SummaryDescription parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static org.tensorflow.framework.SummaryDescription parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(org.tensorflow.framework.SummaryDescription prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * <pre>
   * Metadata associated with a series of Summary data
   * </pre>
   *
   * Protobuf type {@code tensorflow.SummaryDescription}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:tensorflow.SummaryDescription)
      org.tensorflow.framework.SummaryDescriptionOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return org.tensorflow.framework.SummaryProtos.internal_static_tensorflow_SummaryDescription_descriptor;
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return org.tensorflow.framework.SummaryProtos.internal_static_tensorflow_SummaryDescription_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              org.tensorflow.framework.SummaryDescription.class, org.tensorflow.framework.SummaryDescription.Builder.class);
    }

    // Construct using org.tensorflow.framework.SummaryDescription.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      typeHint_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return org.tensorflow.framework.SummaryProtos.internal_static_tensorflow_SummaryDescription_descriptor;
    }

    public org.tensorflow.framework.SummaryDescription getDefaultInstanceForType() {
      return org.tensorflow.framework.SummaryDescription.getDefaultInstance();
    }

    public org.tensorflow.framework.SummaryDescription build() {
      org.tensorflow.framework.SummaryDescription result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public org.tensorflow.framework.SummaryDescription buildPartial() {
      org.tensorflow.framework.SummaryDescription result = new org.tensorflow.framework.SummaryDescription(this);
      result.typeHint_ = typeHint_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof org.tensorflow.framework.SummaryDescription) {
        return mergeFrom((org.tensorflow.framework.SummaryDescription)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(org.tensorflow.framework.SummaryDescription other) {
      if (other == org.tensorflow.framework.SummaryDescription.getDefaultInstance()) return this;
      if (!other.getTypeHint().isEmpty()) {
        typeHint_ = other.typeHint_;
        onChanged();
      }
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      org.tensorflow.framework.SummaryDescription parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (org.tensorflow.framework.SummaryDescription) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private java.lang.Object typeHint_ = "";
    /**
     * <pre>
     * Hint on how plugins should process the data in this series.
     * Supported values include "scalar", "histogram", "image", "audio"
     * </pre>
     *
     * <code>optional string type_hint = 1;</code>
     */
    public java.lang.String getTypeHint() {
      java.lang.Object ref = typeHint_;
      if (!(ref instanceof java.lang.String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        typeHint_ = s;
        return s;
      } else {
        return (java.lang.String) ref;
      }
    }
    /**
     * <pre>
     * Hint on how plugins should process the data in this series.
     * Supported values include "scalar", "histogram", "image", "audio"
     * </pre>
     *
     * <code>optional string type_hint = 1;</code>
     */
    public com.google.protobuf.ByteString
        getTypeHintBytes() {
      java.lang.Object ref = typeHint_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b = 
            com.google.protobuf.ByteString.copyFromUtf8(
                (java.lang.String) ref);
        typeHint_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <pre>
     * Hint on how plugins should process the data in this series.
     * Supported values include "scalar", "histogram", "image", "audio"
     * </pre>
     *
     * <code>optional string type_hint = 1;</code>
     */
    public Builder setTypeHint(
        java.lang.String value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      typeHint_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Hint on how plugins should process the data in this series.
     * Supported values include "scalar", "histogram", "image", "audio"
     * </pre>
     *
     * <code>optional string type_hint = 1;</code>
     */
    public Builder clearTypeHint() {
      
      typeHint_ = getDefaultInstance().getTypeHint();
      onChanged();
      return this;
    }
    /**
     * <pre>
     * Hint on how plugins should process the data in this series.
     * Supported values include "scalar", "histogram", "image", "audio"
     * </pre>
     *
     * <code>optional string type_hint = 1;</code>
     */
    public Builder setTypeHintBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);
      
      typeHint_ = value;
      onChanged();
      return this;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return this;
    }


    // @@protoc_insertion_point(builder_scope:tensorflow.SummaryDescription)
  }

  // @@protoc_insertion_point(class_scope:tensorflow.SummaryDescription)
  private static final org.tensorflow.framework.SummaryDescription DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new org.tensorflow.framework.SummaryDescription();
  }

  public static org.tensorflow.framework.SummaryDescription getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SummaryDescription>
      PARSER = new com.google.protobuf.AbstractParser<SummaryDescription>() {
    public SummaryDescription parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new SummaryDescription(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<SummaryDescription> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<SummaryDescription> getParserForType() {
    return PARSER;
  }

  public org.tensorflow.framework.SummaryDescription getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

