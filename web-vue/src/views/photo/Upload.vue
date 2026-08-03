<template>
  <div class="upload-page" :key="lang">
    <el-card>
      <template #header>{{ t("upload.title") }}</template>

      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        multiple
        :auto-upload="false"
        :on-change="handleFileChange"
        :file-list="fileList"
        accept="image/*,video/*"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          {{ t("upload.dragHint") }} <em>{{ t("upload.clickUpload") }}</em>
        </div>
        <template #tip
          ><div class="el-upload__tip">{{ t("upload.fileTip") }}</div></template
        >
      </el-upload>

      <!-- 文件缩略图预览 -->
      <div v-if="fileList.length > 0" class="file-preview-grid">
        <div
          v-for="(file, idx) in fileList"
          :key="idx"
          class="file-preview-item"
        >
          <img
            v-if="file.raw && file.raw.type.startsWith('image/')"
            :src="getFilePreview(file.raw)"
            class="file-preview-img"
          />
          <div v-else class="file-preview-video">
            <el-icon :size="24"><VideoPlay /></el-icon>
          </div>
          <span class="file-preview-name">{{ file.name }}</span>
        </div>
      </div>

      <div v-if="fileList.length > 0" class="file-options">
        <el-divider>{{ t("upload.options") }}</el-divider>
        <el-form label-width="120px">
          <el-form-item :label="t('upload.addTags')">
            <el-select
              v-model="selectedTags"
              multiple
              filterable
              allow-create
              default-first-option
              :placeholder="t('upload.tagPlaceholder')"
              style="width: 100%"
            >
              <el-option-group
                v-for="(tags, category) in groupedTags"
                :key="category"
                :label="categoryLabel(category)"
              >
                <el-option
                  v-for="tag in tags"
                  :key="tag.id"
                  :label="tag.tagName"
                  :value="tag.id"
                />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item :label="t('upload.newTags')">
            <el-input
              v-model="newTagInput"
              :placeholder="t('upload.newTagPlaceholder')"
            />
            <div v-if="similarTags.length > 0" class="similar-tags-hint">
              <span style="color: #999; font-size: 12px"
                >{{ t("upload.similarTags") }}：</span
              >
              <el-tag
                v-for="tag in similarTags"
                :key="tag.id"
                size="small"
                style="cursor: pointer; margin-right: 4px"
                @click="adoptSimilarTag(tag)"
                >{{ tag.tagName }}</el-tag
              >
            </div>
          </el-form-item>
          <el-form-item :label="t('upload.aiTag')">
            <el-checkbox v-model="aiTag">{{ t("upload.aiTag") }}</el-checkbox>
            <span class="ai-tip">{{ t("upload.aiTip") }}</span>
          </el-form-item>
          <el-form-item :label="t('upload.address')">
            <el-cascader
              v-model="addressValue"
              :options="addressOptions"
              :placeholder="t('upload.addressPlaceholder')"
              clearable
              filterable
              :show-all-levels="false"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
      </div>

      <div class="upload-actions" v-if="fileList.length > 0">
        <el-button @click="clearFiles">{{ t("upload.clear") }}</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload"
          >{{ t("upload.startUpload") }} ({{ fileList.length }})</el-button
        >
      </div>

      <div v-if="uploadProgress.length > 0" class="progress-area">
        <el-divider>{{ t("upload.uploadProgress") }}</el-divider>
        <div
          v-for="(item, idx) in uploadProgress"
          :key="idx"
          class="progress-item"
        >
          <span class="file-name">{{ item.name }}</span>
          <el-progress
            :percentage="item.progress"
            :status="item.status"
            style="flex: 1; margin: 0 12px"
          />
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="aiTagDialogVisible"
      :title="t('upload.aiDialogTitle')"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="aiTagLoading" class="ai-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>{{ t("upload.aiLoading") }}</p>
      </div>
      <div v-else>
        <el-alert
          v-if="aiResult.city || aiResult.province"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 16px"
        >
          {{ t("upload.aiLocation") }}{{ aiResult.province || "" }}
          {{ aiResult.city || "" }}
        </el-alert>
        <p style="margin-bottom: 12px; color: #666">{{t("upload.description")}}</p>
        <p style="margin-bottom: 12px; color: #666">{{aiResult.description}}</p>

        <p style="margin-bottom: 12px; color: #666">
          {{ t("upload.aiSelectHint") }}
        </p>
        <el-checkbox-group v-model="selectedAiTags">
          <div class="ai-tag-list">
            <el-checkbox
              v-for="tag in aiResult.tags"
              :key="tag"
              :value="tag"
              class="ai-tag-item"
            >
              <el-tag size="small">{{ tag }}</el-tag>
            </el-checkbox>
          </div>
        </el-checkbox-group>
        <div style="margin-top: 16px">
          <p style="margin-bottom: 8px; color: #999; font-size: 12px">
            {{ t("upload.aiExtraTags") }}
          </p>
          <el-input
            v-model="extraAiTags"
            :placeholder="t('upload.aiExtraPlaceholder')"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="skipAiTags">{{ t("upload.aiSkip") }}</el-button>
        <el-button
          type="primary"
          @click="confirmAiTagSelection"
          :disabled="aiTagLoading"
          >{{ t("upload.aiConfirm") }} ({{
            selectedAiTags.length + extraAiTagsCount
          }})</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import {
  uploadPhotos,
  getTagList,
  getAiSuggestedTags,
  confirmAiTags,
} from "../../api";
import { useI18n } from "../../utils/i18n";
import { ElMessage } from "element-plus";

const { t, lang } = useI18n();
const fileList = ref([]);
const tagList = ref([]);
const selectedTags = ref([]);
const newTagInput = ref("");
const aiTag = ref(true);
const uploading = ref(false);
const uploadProgress = ref([]);
const aiTagDialogVisible = ref(false);
const aiTagLoading = ref(false);
const aiResult = ref({ tags: [], city: null, province: null });
const selectedAiTags = ref([]);
const extraAiTags = ref("");
const currentPhotoId = ref(null);
const addressValue = ref([]);

const groupedTags = computed(() => {
  const groups = {};
  for (const tag of tagList.value) {
    const cat = tag.tagCategory || "other";
    if (!groups[cat]) groups[cat] = [];
    groups[cat].push(tag);
  }
  return groups;
});

const extraAiTagsCount = computed(
  () => extraAiTags.value.split(/[,，]/).filter((x) => x.trim()).length,
);

// 相似标签检测
const similarTags = ref([]);
watch(newTagInput, (val) => {
  if (!val || !val.trim()) {
    similarTags.value = [];
    return;
  }
  const input = val.trim().toLowerCase();
  similarTags.value = tagList.value
    .filter((tag) => {
      const name = tag.tagName.toLowerCase();
      return (
        name !== input &&
        (name.includes(input) ||
          input.includes(name) ||
          levenshtein(name, input) <= 2)
      );
    })
    .slice(0, 5);
});

function levenshtein(a, b) {
  const m = a.length,
    n = b.length;
  const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));
  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;
  for (let i = 1; i <= m; i++)
    for (let j = 1; j <= n; j++)
      dp[i][j] = Math.min(
        dp[i - 1][j] + 1,
        dp[i][j - 1] + 1,
        dp[i - 1][j - 1] + (a[i - 1] !== b[j - 1] ? 1 : 0),
      );
  return dp[m][n];
}

function adoptSimilarTag(tag) {
  if (!selectedTags.value.includes(tag.id)) {
    selectedTags.value.push(tag.id);
  }
  newTagInput.value = "";
  similarTags.value = [];
}

const categoryLabels = {
  province: "省/自治区",
  city: "城市",
  landscape: "风景",
  scene: "场景",
  food: "美食",
  people: "人物",
  other: "其他",
};
function categoryLabel(cat) {
  return categoryLabels[cat] || cat;
}

const addressOptions = [
  {
    value: "北京",
    label: "北京市",
    children: [{ value: "北京", label: "北京" }],
  },
  {
    value: "上海",
    label: "上海市",
    children: [{ value: "上海", label: "上海" }],
  },
  {
    value: "天津",
    label: "天津市",
    children: [{ value: "天津", label: "天津" }],
  },
  {
    value: "重庆",
    label: "重庆市",
    children: [{ value: "重庆", label: "重庆" }],
  },
  {
    value: "河北",
    label: "河北省",
    children: [
      { value: "石家庄", label: "石家庄" },
      { value: "唐山", label: "唐山" },
      { value: "秦皇岛", label: "秦皇岛" },
      { value: "保定", label: "保定" },
    ],
  },
  {
    value: "山西",
    label: "山西省",
    children: [
      { value: "太原", label: "太原" },
      { value: "大同", label: "大同" },
    ],
  },
  {
    value: "内蒙古",
    label: "内蒙古自治区",
    children: [
      { value: "呼和浩特", label: "呼和浩特" },
      { value: "包头", label: "包头" },
    ],
  },
  {
    value: "辽宁",
    label: "辽宁省",
    children: [
      { value: "沈阳", label: "沈阳" },
      { value: "大连", label: "大连" },
      { value: "鞍山", label: "鞍山" },
    ],
  },
  {
    value: "吉林",
    label: "吉林省",
    children: [
      { value: "长春", label: "长春" },
      { value: "吉林", label: "吉林" },
    ],
  },
  {
    value: "黑龙江",
    label: "黑龙江省",
    children: [
      { value: "哈尔滨", label: "哈尔滨" },
      { value: "大庆", label: "大庆" },
    ],
  },
  {
    value: "江苏",
    label: "江苏省",
    children: [
      { value: "南京", label: "南京" },
      { value: "苏州", label: "苏州" },
      { value: "无锡", label: "无锡" },
      { value: "常州", label: "常州" },
    ],
  },
  {
    value: "浙江",
    label: "浙江省",
    children: [
      { value: "杭州", label: "杭州" },
      { value: "宁波", label: "宁波" },
      { value: "温州", label: "温州" },
      { value: "嘉兴", label: "嘉兴" },
    ],
  },
  {
    value: "安徽",
    label: "安徽省",
    children: [
      { value: "合肥", label: "合肥" },
      { value: "芜湖", label: "芜湖" },
    ],
  },
  {
    value: "福建",
    label: "福建省",
    children: [
      { value: "福州", label: "福州" },
      { value: "厦门", label: "厦门" },
      { value: "泉州", label: "泉州" },
    ],
  },
  {
    value: "江西",
    label: "江西省",
    children: [
      { value: "南昌", label: "南昌" },
      { value: "九江", label: "九江" },
    ],
  },
  {
    value: "山东",
    label: "山东省",
    children: [
      { value: "济南", label: "济南" },
      { value: "青岛", label: "青岛" },
      { value: "烟台", label: "烟台" },
    ],
  },
  {
    value: "河南",
    label: "河南省",
    children: [
      { value: "郑州", label: "郑州" },
      { value: "洛阳", label: "洛阳" },
      { value: "开封", label: "开封" },
    ],
  },
  {
    value: "湖北",
    label: "湖北省",
    children: [
      { value: "武汉", label: "武汉" },
      { value: "宜昌", label: "宜昌" },
      { value: "襄阳", label: "襄阳" },
    ],
  },
  {
    value: "湖南",
    label: "湖南省",
    children: [
      { value: "长沙", label: "长沙" },
      { value: "岳阳", label: "岳阳" },
      { value: "张家界", label: "张家界" },
    ],
  },
  {
    value: "广东",
    label: "广东省",
    children: [
      { value: "广州", label: "广州" },
      { value: "深圳", label: "深圳" },
      { value: "东莞", label: "东莞" },
      { value: "佛山", label: "佛山" },
    ],
  },
  {
    value: "广西",
    label: "广西壮族自治区",
    children: [
      { value: "南宁", label: "南宁" },
      { value: "桂林", label: "桂林" },
      { value: "柳州", label: "柳州" },
    ],
  },
  {
    value: "海南",
    label: "海南省",
    children: [
      { value: "海口", label: "海口" },
      { value: "三亚", label: "三亚" },
    ],
  },
  {
    value: "四川",
    label: "四川省",
    children: [
      { value: "成都", label: "成都" },
      { value: "绵阳", label: "绵阳" },
      { value: "九寨沟", label: "九寨沟" },
    ],
  },
  {
    value: "贵州",
    label: "贵州省",
    children: [
      { value: "贵阳", label: "贵阳" },
      { value: "遵义", label: "遵义" },
    ],
  },
  {
    value: "云南",
    label: "云南省",
    children: [
      { value: "昆明", label: "昆明" },
      { value: "大理", label: "大理" },
      { value: "丽江", label: "丽江" },
      { value: "西双版纳", label: "西双版纳" },
    ],
  },
  {
    value: "西藏",
    label: "西藏自治区",
    children: [
      { value: "拉萨", label: "拉萨" },
      { value: "日喀则", label: "日喀则" },
    ],
  },
  {
    value: "陕西",
    label: "陕西省",
    children: [
      { value: "西安", label: "西安" },
      { value: "咸阳", label: "咸阳" },
      { value: "延安", label: "延安" },
    ],
  },
  {
    value: "甘肃",
    label: "甘肃省",
    children: [
      { value: "兰州", label: "兰州" },
      { value: "敦煌", label: "敦煌" },
    ],
  },
  {
    value: "青海",
    label: "青海省",
    children: [
      { value: "西宁", label: "西宁" },
      { value: "青海湖", label: "青海湖" },
    ],
  },
  {
    value: "宁夏",
    label: "宁夏回族自治区",
    children: [{ value: "银川", label: "银川" }],
  },
  {
    value: "新疆",
    label: "新疆维吾尔自治区",
    children: [
      { value: "乌鲁木齐", label: "乌鲁木齐" },
      { value: "喀什", label: "喀什" },
      { value: "吐鲁番", label: "吐鲁番" },
    ],
  },
  {
    value: "台湾",
    label: "台湾省",
    children: [
      { value: "台北", label: "台北" },
      { value: "高雄", label: "高雄" },
    ],
  },
  {
    value: "香港",
    label: "香港特别行政区",
    children: [{ value: "香港", label: "香港" }],
  },
  {
    value: "澳门",
    label: "澳门特别行政区",
    children: [{ value: "澳门", label: "澳门" }],
  },
];

onMounted(async () => {
  const res = await getTagList();
  if (res.code === 200) tagList.value = res.data;
});
function handleFileChange(file, list) {
  fileList.value = list;
}
function clearFiles() {
  fileList.value = [];
  uploadProgress.value = [];
}

const filePreviewCache = ref({});
function getFilePreview(file) {
  if (!filePreviewCache.value[file.uid]) {
    filePreviewCache.value[file.uid] = URL.createObjectURL(file);
  }
  return filePreviewCache.value[file.uid];
}

async function handleUpload() {
  if (fileList.value.length === 0) return;
  uploading.value = true;
  uploadProgress.value = fileList.value.map((f) => ({
    name: f.name,
    progress: 0,
    status: "",
  }));
  try {
    const formData = new FormData();
    fileList.value.forEach((f) => formData.append("files", f.raw));
    if (selectedTags.value.length > 0)
      formData.append("tagIds", selectedTags.value.join(","));
    if (newTagInput.value.trim())
      formData.append("newTags", newTagInput.value.trim());
    formData.append("aiTag", aiTag.value);
    if (addressValue.value && addressValue.value.length >= 2) {
      formData.append("province", addressValue.value[0]);
      formData.append("city", addressValue.value[1]);
    }
    const res = await uploadPhotos(formData);
    if (res.code === 200) {
      uploadProgress.value.forEach((p) => {
        p.progress = 100;
        p.status = "success";
      });
      const hasVideo = fileList.value.some(
        (f) => f.raw && f.raw.type.startsWith("video/"),
      );
      const msgKey = hasVideo
        ? "upload.uploadSuccessVideo"
        : "upload.uploadSuccess";
      ElMessage.success(t(msgKey, { count: res.data.length }));
      if (aiTag.value && res.data.length > 0 && !hasVideo) {
        currentPhotoId.value = res.data[0].id;
        showAiTagDialog(res.data[0].id);
      }
      fileList.value = [];
    } else {
      throw new Error(res.message);
    }
  } catch (e) {
    uploadProgress.value.forEach((p) => {
      p.status = "exception";
    });
    ElMessage.error(t("upload.uploadFailed") + ": " + (e.message || ""));
  } finally {
    uploading.value = false;
  }
}

async function showAiTagDialog(photoId) {
  aiTagDialogVisible.value = true;
  aiTagLoading.value = true;
  selectedAiTags.value = [];
  extraAiTags.value = "";
  try {
    const res = await getAiSuggestedTags(photoId);
    if (res.code === 200) {
      aiResult.value = res.data;
      selectedAiTags.value = [...(res.data.tags || [])];
    }
  } catch {
    ElMessage.error(t("upload.aiFailed"));
    aiResult.value = {
      tags: [],
      city: null,
      province: null,
      description: null,
    };
  } finally {
    aiTagLoading.value = false;
  }
}

async function confirmAiTagSelection() {
  const allTags = [...selectedAiTags.value];
  extraAiTags.value.split(/[,，]/).forEach((x) => {
    const v = x.trim();
    if (v && !allTags.includes(v)) allTags.push(v);
  });
  try {
    await confirmAiTags(currentPhotoId.value, {
      tags: allTags,
      city: aiResult.value.city,
      province: aiResult.value.province,
      description: aiResult.value.description,
    });
    ElMessage.success(t("upload.tagsSaved"));
    aiTagDialogVisible.value = false;
  } catch {
    ElMessage.error(t("common.failed"));
  }
}

function skipAiTags() {
  aiTagDialogVisible.value = false;
}
</script>

<style scoped>
.upload-area {
  width: 100%;
}
.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  padding: 40px 0;
}
.file-options {
  margin-top: 20px;
}
.ai-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #999;
}
.upload-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
.progress-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.file-name {
  width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #666;
}
.ai-loading {
  text-align: center;
  padding: 40px 0;
  color: #999;
}
.ai-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.ai-tag-item {
  margin-right: 0 !important;
}
.file-preview-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}
.file-preview-item {
  width: 100px;
  text-align: center;
}
.file-preview-img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #eee;
}
.file-preview-video {
  width: 100px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #eee;
  color: #999;
}
.file-preview-name {
  display: block;
  font-size: 11px;
  color: #666;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.similar-tags-hint {
  margin-top: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
